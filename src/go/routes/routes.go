package routes

import (
	"Orbit-Messenger/src/go/db"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/golang/glog"
	"github.com/gorilla/websocket"
	"time"
)

const (
	IMAGE_FOLDER_PATH = "./src/res/images/"
)

// ServerStateController controls the database for each route
type ServerStateController struct {
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
	Password      string                 `json:"password"`
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
	AllUsers      []db.User
}

// CreateServerStateController will create a database connection and return a ServerStateController
func CreateServerStateController() ServerStateController {
	return ServerStateController{
		db.CreateDatabaseConnection(),
		CreateServerActionsController(),
	}
}

func (serverState ServerStateController) WebSocket(c *gin.Context) {
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		fmt.Println(err)
	}
	conn.SetReadDeadline(time.Now().Add(60 * time.Second))

	var state State
	state.Chatroom = "general"

	go serverState.handleAction(conn, &state)
	go serverState.UpdateHandler(conn, &state)
}

func (serverState ServerStateController) VerifyUser(c *gin.Context) {
	var user Auth
	bindErr := c.BindJSON(&user)
	if bindErr != nil {
		glog.Warning("Bind Err: ", bindErr.Error())
	}

	glog.Infof("Verifying user: %v", user)

	if serverState.dbConn.VerifyPasswordByUsername(user.Username, user.Password) {
		c.Status(200)
	} else {
		c.Status(403)
	}
}

func (serverState ServerStateController) CreateUser(c *gin.Context) {
	var user Auth
	bindErr := c.BindJSON(&user)
	if bindErr != nil {
		glog.Warning("Bind Err: ", bindErr.Error())
	}
	if !serverState.dbConn.CheckIfUserExists(user.Username) {
		serverState.dbConn.CreateUser(user.Username, user.Password)
	}
	if serverState.dbConn.VerifyPasswordByUsername(user.Username, user.Password) {
		c.Status(200)
	} else {
		c.Status(500)
	}
}

func (serverState ServerStateController) CreateChatroom(c *gin.Context) {
	var chatroom db.Chatroom
	// Shouldn't ignore err.
	bindErr := c.BindJSON(&chatroom)
	if bindErr != nil {
		glog.Warning("Bind Err: ", bindErr.Error())
	}
	if !serverState.dbConn.CheckIfChatroomExists(chatroom.Name) {
		serverState.dbConn.CreateChatroom(chatroom.Name)
	}
	if serverState.dbConn.CheckIfChatroomExists(chatroom.Name) {
		c.Status(200)
	} else {
		c.Status(500)
	}
}

func (serverState ServerStateController) ChangePassword(c *gin.Context) {
	var user db.User
	// Shouldn't ignore err.
	bindErr := c.BindJSON(&user)
	if bindErr != nil {
		glog.Warning("Bind Err: ", bindErr.Error())
	}

	id, err := serverState.dbConn.GetUserId(user.Username)
	if err != nil {
		glog.Warning("database error couldn't get user id: %v", err)
	}

	err = serverState.dbConn.ChangePassword(id, user.Password)
	if err != nil {
		glog.Warning("database error couldn't change user password: %v", err)
	}
	c.Status(200)
}

func (serverState ServerStateController) getAllUsers() db.AllUsers {
	users, err := serverState.dbConn.GetAllUsers()
	if err != nil {
		glog.Error(err)
		return users
	}
	return users
}

func (serverState ServerStateController) getActiveUsersForClient(chatroom string) db.ActiveUsers {
	activeUsers, err := serverState.dbConn.GetUsersByStatus(true, chatroom)
	if err != nil {
		glog.Error(err)
		return activeUsers
	}
	return activeUsers
}

func (serverState ServerStateController) getNewMessagesForClient(lastMessageId *int64, chatroom *string, users *[]string, messageLimit *int64) db.Messages {
	// If Chatroom direct message, get those messages, else get the regular room messages.
	if *chatroom == "direct_messages" {
		messages, err := serverState.dbConn.GetNewestDirectMessages(*lastMessageId, *users, *messageLimit)
		if err != nil {
			glog.Error(err)
			return messages
		}
		updateLastMessageId(messages.Messages, lastMessageId)
		return messages

	} else {
		messages, err := serverState.dbConn.GetNewestMessagesFrom(*lastMessageId, *chatroom, *messageLimit)
		if err != nil {
			glog.Error(err)
			return messages
		}
		updateLastMessageId(messages.Messages, lastMessageId)
		return messages
	}
}

// Gets all the messages for the client
func (serverState ServerStateController) getAllMessagesForClient(lastMessageId *int64, chatroom *string, users []string, messageLimit *int64) db.Messages {
	if *chatroom == "direct_messages" {
		messages, err := serverState.dbConn.GetAllDirectMessages(users[0], users[1], *messageLimit)
		if err != nil {
			glog.Error(err)
			return messages
		}
		updateLastMessageId(messages.Messages, lastMessageId)
		return messages
	} else {
		messages, err := serverState.dbConn.GetAllMessages(*chatroom, *messageLimit)
		if err != nil {
			glog.Error(err)
			return messages
		}
		updateLastMessageId(messages.Messages, lastMessageId)
		return messages

	}
}

// Adds an avatar image to the image folder and updates the location in the database
func (serverState ServerStateController) AddAvatarToUser(c *gin.Context) {
	var user Auth

	// converts base64 to an auth
	basicAuth := c.GetHeader("Authorization")
	user, err := serverState.GetUsernameAndPasswordFromBase64(basicAuth)
	if err != nil {
		glog.Error("couldn't decode base64 auth")
		return
	}

	// Authenticates the user
	if serverState.dbConn.VerifyPasswordByUsername(user.Username, user.Password) {
		serverState.createAvatarImgAndDataEntry(user.Username, c)
	} else {
		c.Status(500)
	}
}

func (serverState ServerStateController) createAvatarImgAndDataEntry(username string, c *gin.Context) {
	// Gets a file from the http request
	file, err := c.FormFile("file")
	if err != nil {
		glog.Error(err)
		c.Status(400)
		return
	}

	// Saves the file to the image folder
	err = c.SaveUploadedFile(file, IMAGE_FOLDER_PATH+file.Filename)
	if err != nil {
		glog.Error(err)
	}

	// Updates the database with the image location
	err = serverState.dbConn.AddAvatar(username, IMAGE_FOLDER_PATH+file.Filename)
	if err != nil {
		glog.Error(err)
	}
	c.Status(200)
	glog.Error(err)
}

type user struct {
	Username string `form:"username"`
}

func (serverState ServerStateController) GetAvatar(c *gin.Context) {
	defaultImg := "./src/res/images/default.jpg"
	var username user
	basicAuth := c.GetHeader("Authorization")
	user, err := serverState.GetUsernameAndPasswordFromBase64(basicAuth)
	if err != nil {
		glog.Error("couldn't decode base64 auth")
		return
	}

	// Authenticates the user
	if serverState.dbConn.VerifyPasswordByUsername(user.Username, user.Password) {
		err = c.BindQuery(&username)
		if err != nil {
			glog.Error(err)
			c.String(404, "error %v", err)
		}
		avatarInfo, err := serverState.dbConn.GetAvatarByUsername(username.Username)
		if err != nil {
			c.String(201, "error %v", err)
			avatarInfo.Location = defaultImg
		}
		c.File(avatarInfo.Location)
	} else {
		c.Status(500)
	}
}

func updateLastMessageId(messages []db.Message, lastMessageId *int64) {
	if len(messages) < 1 {
		return
	}
	if *lastMessageId < messages[0].MessageId {
		*lastMessageId = messages[0].MessageId
	}
}
