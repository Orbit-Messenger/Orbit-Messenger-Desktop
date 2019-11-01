package routes

import (
	"Orbit-Messenger/src/go/db"
	"encoding/base64"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
	"log"
	"strings"
)

// RouteController controls the database for each route
type RouteController struct {
	dbConn db.DatabaseConnection
}

// Auth is used to validate the username and password given in an http header
type Auth struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

type ClientData struct {
	Message       string                 `json:"message"`
	Username      string                 `json:"username"`
	Action        string                 `json:"action"`
	LastMessageId int64                  `json:"lastMessageId"`
	Properties    map[string]interface{} `json:"properties"`
}

// CreateRouteController will create a database connection and return a RouteController
func CreateRouteController() RouteController {
	return RouteController{
		db.CreateDatabaseConnection(),
	}
}

// Used to get the username and password from basic auth
func getUsernameAndPasswordFromBase64(input string) (Auth, error) {
	var output Auth
	if input == "" {
		return output, fmt.Errorf("No username or password in basic auth")
	}

	// basic auth will give a string looking like basic YmFzaWMgYnJvZHk6dGVzdA==
	// this removes the basic part
	base64String := strings.Replace(input, "Basic ", "", 1)
	data, err := base64.StdEncoding.DecodeString(base64String)
	fmt.Printf("\ndata: %v\n", string(data))
	if err != nil {
		return output, err
	}
	usernameAndPassword := strings.Split(string(data), ":")
	output = Auth{usernameAndPassword[0], usernameAndPassword[1]}
	return output, nil
}

func (rc RouteController) handleAction(conn *websocket.Conn) {
	for {
		var clientData ClientData
		err := conn.ReadJSON(&clientData)
		if err != nil {
			return
		}
		fmt.Println(clientData)
		fmt.Println(clientData.Action)
		switch clientData.Action {
		case "getAllMessages":
			log.Println("getting All Messages")
			messages, err := rc.dbConn.GetAllMessages()
			if err != nil {
				conn.WriteJSON(err)
			} else {
				conn.WriteJSON(messages)
			}
		case "add":
			log.Println("Adding message")
			message := db.Message{-1, clientData.Username, clientData.Message}
			err := rc.dbConn.AddMessage(message, message.Username)
			if err != nil {
				conn.WriteJSON(err)
			} else {
				messages, err := rc.dbConn.GetNewestMessagesFrom(clientData.LastMessageId)
				if err != nil {
					conn.WriteJSON(err)
				} else {
					conn.WriteJSON(messages)
				}
			}
		default:
			conn.WriteMessage(websocket.PongMessage, []byte("pong"))
		}
	}
	fmt.Println("Out of handleAction")
}

func (rc RouteController) WebSocket(c *gin.Context) {
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		fmt.Println(err)
	}
	go rc.handleAction(conn)
}

// Creates an Auth from the given context by it's header
func (rc RouteController) getAuth(c *gin.Context) (Auth, error) {
	auth, err := getUsernameAndPasswordFromBase64(c.GetHeader("Authorization"))
	if err != nil {
		return *new(Auth), nil
	}
	return auth, nil
}

// validates the context headers basic auth against the usernames and passwords in the database
func (rc RouteController) ValidateUser(c *gin.Context) bool {
	auth, err := rc.getAuth(c)
	if err != nil {
		return false
	}
	return rc.dbConn.VerifyPasswordByUsername(auth.Username, auth.Password)
}
