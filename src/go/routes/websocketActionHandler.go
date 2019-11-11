package routes

import (
	"Orbit-Messenger/src/go/db"
	_ "fmt"
	"github.com/gorilla/websocket"
	"strconv"
)

// handles all the actions from the requesting client
func (rc *RouteController) handleAction(wsConn *websocket.Conn, state *State) {
	for {
		var clientData ClientData
		err := wsConn.ReadJSON(&clientData)
		if err != nil {
			return
		}
		switch clientData.Action {
		case "login":
			rc.dbConn.ChangeUserStatus(clientData.Username, true)
			state.LoggedIn = true
			state.Username = clientData.Username
		case "logout":
			rc.dbConn.ChangeUserStatus(clientData.Username, false)
			state.LoggedIn = false
			state.LoggedOut = true
			wsConn.Close()
			return
		case "add":
			rc.addMessageFromClient(clientData, state.Username)
		case "delete":
			rc.deleteMessageFromClient(clientData, state.Username)
		default:
			wsConn.WriteMessage(websocket.PongMessage, []byte("pong"))
		}
	}
}

// Gets all the messages for the client
func (rc RouteController) addMessageFromClient(clientData ClientData, username string) {
	//log.Println("Adding message")
	message := db.Message{-1, username, clientData.Message}
	rc.dbConn.AddMessage(message, message.Username)
}

// Gets all the messages for the client
func (rc RouteController) deleteMessageFromClient(clientData ClientData, username string) bool {
	//log.Println("Deleting message")
	messageId, err := strconv.ParseInt(clientData.Message, 10, 64)
	if err != nil {
		return false
	}
	userOfTheMessage, err := rc.dbConn.GetUsernameFromMessageId(messageId)
	if userOfTheMessage == username && err == nil {
		return rc.dbConn.DeleteMessageById(messageId)
	}
	return false
}
