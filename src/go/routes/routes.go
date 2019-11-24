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

func (rc RouteController) ChangePassword(c *gin.Context) {
	var user db.User
	c.BindJSON(&user)
	id, err := rc.dbConn.GetUserId(user.Username)
	if err != nil {
		log.Println(err)
	}

	err = rc.dbConn.ChangePassword(id, user.Password)
	if err != nil {
		log.Println(err)
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

func (rc RouteController) getNewMessagesForClient(lastMessageId *int64, chatroom *string) db.Messages {
	messages, err := rc.dbConn.GetNewestMessagesFrom(*lastMessageId, *chatroom)
	if err != nil {
		log.Println(err)
		return messages
	}

	updateLastMessageId(messages.Messages, lastMessageId)
	return messages
}

// Gets all the messages for the client
func (rc RouteController) getAllMessagesForClient(lastMessageId *int64, chatroom *string) db.Messages {
	log.Println("getting All Messages")
	messages, err := rc.dbConn.GetAllMessages(*chatroom)
	if err != nil {
		log.Println(err)
		return messages
	}

	updateLastMessageId(messages.Messages, lastMessageId)
	return messages
}

func updateLastMessageId(messages []db.Message, lastMessageId *int64) {
	if len(messages) < 1 {
		return
	}
	*lastMessageId = messages[len(messages)-1].MessageId
}
