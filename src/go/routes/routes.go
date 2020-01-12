package routes

import (
	"Orbit-Messenger/src/go/db"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/golang/glog"
	"github.com/gorilla/websocket"
	"time"
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
	Users         []string               `json:"users"`
	Action        string                 `json:"action"`
	LastMessageId int64                  `json:"lastMessageId"`
	Properties    map[string]interface{} `json:"properties"`
}

type FullData struct {
	Messages  []db.Message  `json:"messages"`
	AllUsers  []db.User     `json:"allUsers"`
	Chatrooms []db.Chatroom `json:"chatrooms"`
}

type State struct {
	LastMessageId int64
	Username      string
	Users         []string
	Chatroom      string
	LoggedIn      bool
	LoggedOut     bool
	MessageLimit  int64
	AllUsers   	  []db.User
}

// CreateRouteController will create a database connection and return a RouteController
func CreateRouteController() RouteController {
	return RouteController{
		db.CreateDatabaseConnection(),
		CreateServerActionsController(),
	}
}

func (rc RouteController) WebSocket(c *gin.Context) {
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		fmt.Println(err)
	}
	conn.SetReadDeadline(time.Now().Add(60 * time.Second))

	var state State
	state.Chatroom = "general"

	go rc.handleAction(conn, &state)
	go rc.UpdateHandler(conn, &state)
}

func (rc RouteController) VerifyUser(c *gin.Context) {
	var user Auth
	bindErr := c.BindJSON(&user)
	if bindErr != nil {
		glog.Warning("Bind Err: ", bindErr.Error())
	}

	glog.Infof("Verifying user: %v", user)

	if rc.dbConn.VerifyPasswordByUsername(user.Username, user.Password) {
		c.Status(200)
	} else {
		c.Status(403)
	}
}

func (rc RouteController) CreateUser(c *gin.Context) {
	var user Auth
	bindErr := c.BindJSON(&user)
	if bindErr != nil {
		glog.Warning("Bind Err: ", bindErr.Error())
	}
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
	// Shouldn't ignore err.
	bindErr := c.BindJSON(&chatroom)
	if bindErr != nil {
		glog.Warning("Bind Err: ", bindErr.Error())
	}
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
	// Shouldn't ignore err.
	bindErr := c.BindJSON(&user)
	if bindErr != nil {
		glog.Warning("Bind Err: ", bindErr.Error())
	}

	id, err := rc.dbConn.GetUserId(user.Username)
	if err != nil {
		glog.Warning("database error couldn't get user id: %v", err)
	}

	err = rc.dbConn.ChangePassword(id, user.Password)
	if err != nil {
		glog.Warning("database error couldn't change user password: %v", err)
	}
}

func (rc RouteController) getAllUsers() []db.User {
	users, err := rc.dbConn.GetAllUsers()
	if err != nil {
		glog.Error(err)
		return users
	}
	return users
}

func (rc RouteController) getActiveUsersForClient(chatroom string) db.ActiveUsers {
	activeUsers, err := rc.dbConn.GetUsersByStatus(true, chatroom)
	if err != nil {
		glog.Error(err)
		return activeUsers
	}
	return activeUsers
}

func (rc RouteController) getNewMessagesForClient(lastMessageId *int64, chatroom *string, users *[]string, messageLimit *int64) db.Messages {
	// If Chatroom direct message, get those messages, else get the regular room messages.
	if *chatroom == "direct_message" {
		messages, err := rc.dbConn.GetNewestDirectMessagesFrom(*lastMessageId, *users, *messageLimit)
		if err != nil {
			glog.Error(err)
			return messages
		}
		updateLastMessageId(messages.Messages, lastMessageId)
		return messages
	} else {
		messages, err := rc.dbConn.GetNewestMessagesFrom(*lastMessageId, *chatroom, *messageLimit)
		if err != nil {
			glog.Error(err)
			return messages
		}
		updateLastMessageId(messages.Messages, lastMessageId)
		return messages
	}
}

// Gets all the messages for the client
func (rc RouteController) getAllMessagesForClient(lastMessageId *int64, chatroom *string, users *[]string, messageLimit *int64) db.Messages {
	glog.Info("getting All Messages")
	messages, err := rc.dbConn.GetAllMessages(*chatroom, *messageLimit)
	if err != nil {
		glog.Error(err)
		return messages
	}
	updateLastMessageId(messages.Messages, lastMessageId)
	return messages
}

func updateLastMessageId(messages []db.Message, lastMessageId *int64) {
	if len(messages) < 1 {
		return
	}
	*lastMessageId = messages[0].MessageId
	//*lastMessageId = messages[len(messages)-1].MessageId
}
