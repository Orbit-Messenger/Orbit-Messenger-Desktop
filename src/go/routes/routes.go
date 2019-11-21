package routes

import (
	"Orbit-Messenger/src/go/db"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
	"log"
)

// RouteController controls the database for each route
type RouteController struct {
	dbConn        db.DatabaseConnection
	serverActions *ServerActionsController
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

type ClientData struct {
	Message       string                 `json:"message"`
	Chatroom      string                 `json:"chatroom"`
	Username      string                 `json:"username"`
	Action        string                 `json:"action"`
	LastMessageId int64                  `json:"lastMessageId"`
	Properties    map[string]interface{} `json:"properties"`
}

//type ServerResponse struct {
//	ActiveUsers []string     `json:"activeUsers"`
//	Messages    []db.Message `json:"messages"`
//	Errors      string       `json:"errors"`
//}

type FullData struct {
	Messages    []db.Message  `json:"messages"`
	ActiveUsers []string      `json:"activeUsers"`
	Chatrooms   []db.Chatroom `json:"chatrooms"`
}

type State struct {
	LastMessageId int64
	Username      string
	Chatroom      string
	LoggedIn      bool
	LoggedOut     bool
}

// CreateRouteController will create a database connection and return a RouteController
func CreateRouteController() RouteController {
	return RouteController{
		db.CreateDatabaseConnection(),
		CreateServerActionsController(),
	}
}

func (rc RouteController) VerifyUser(c *gin.Context) {
	var user Auth
	c.BindJSON(&user)
	log.Println(user)
	if rc.dbConn.VerifyPasswordByUsername(user.Username, user.Password) {
		c.Status(200)
	} else {
		c.Status(403)
	}
}

func (rc RouteController) CreateUser(c *gin.Context) {
	var user Auth
	c.BindJSON(&user)
	if !rc.dbConn.CheckIfUserExists(user.Username) {
		rc.dbConn.CreateUser(user.Username, user.Password)
	}
	if rc.dbConn.VerifyPasswordByUsername(user.Username, user.Password) {
		c.Status(200)
	} else {
		c.Status(500)
	}
}

func (rc RouteController) CreateChatroom(c *gin.Context) {
	var chatroom db.Chatroom
	c.BindJSON(&chatroom)
	if !rc.dbConn.CheckIfChatroomExists(chatroom.Name) {
		rc.dbConn.CreateChatroom(chatroom.Name)
	}
	if rc.dbConn.CheckIfChatroomExists(chatroom.Name) {
		c.Status(200)
	} else {
		c.Status(500)
	}
}

func (rc RouteController) WebSocket(c *gin.Context) {
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		fmt.Println(err)
	}

	var state State
	state.Chatroom = "general"

	go rc.handleAction(conn, &state)
	go rc.UpdateHandler(conn, &state)
}
