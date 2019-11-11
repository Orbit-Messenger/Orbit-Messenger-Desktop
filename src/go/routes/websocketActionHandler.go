package routes

import (
	"Orbit-Messenger/src/go/db"
	_ "fmt"
	"github.com/gorilla/websocket"
	"log"
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
			log.Println("deleting messenge")
			rc.deleteMessageFromClient(clientData, state.Username)
		default:
			wsConn.WriteMessage(websocket.PongMessage, []byte("pong"))
		}
	}
}

// Gets all the messages for the client
func (rc RouteController) addMessageFromClient(clientData ClientData, username string) {
	message := db.Message{-1, username, clientData.Message}
	rc.dbConn.AddMessage(message, message.Username)
}

// Gets all the messages for the client
func (rc RouteController) deleteMessageFromClient(clientData ClientData, username string) {
	messageId, err := strconv.ParseInt(clientData.Message, 10, 64)
	if err != nil {
		return
	}
	userOfTheMessage := rc.dbConn.GetUsernameFromMessageId(messageId)
	if userOfTheMessage == username {
		rc.serverActions.AddDeleteAction(messageId)
		rc.dbConn.DeleteMessageById(messageId)
	}
}
