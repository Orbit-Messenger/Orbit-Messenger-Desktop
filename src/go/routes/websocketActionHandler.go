package routes

import (
	"Orbit-Messenger/src/go/db"
	"fmt"
	_ "fmt"
	"github.com/gorilla/websocket"
	"log"
	"strconv"
	"time"
)

// handles all the actions from the requesting client
func (rc *RouteController) handleAction(wsConn *websocket.Conn, state *State) {
	for {
		var clientData ClientData
		err := wsConn.ReadJSON(&clientData)
		if err != nil {
			return
		}
		fmt.Println("Action: ", clientData.Action)
		switch clientData.Action {
		case "login":
			fmt.Println("Logging in!")
			rc.dbConn.ChangeUserStatus(clientData.Username, true)
			state.LoggedIn = true
			state.Username = clientData.Username
		case "logout":
			fmt.Println("Closing! " + clientData.Username)
			rc.dbConn.ChangeUserStatus(clientData.Username, false)
			state.LoggedIn = false
			state.LoggedOut = true
			wsConn.Close()
			return
		case "add":
			rc.addMessageFromClient(clientData, state.Username)
		case "delete":
			log.Println("deleting message")
			rc.deleteMessageFromClient(clientData, state.Username)
		default:
			wsConn.WriteMessage(websocket.PongMessage, []byte("pong"))
		}
	}
}

// Gets all the messages for the client
func (rc RouteController) addMessageFromClient(clientData ClientData, username string) {
	message := db.Message{-1, username, "1", clientData.Message, time.Now()}
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
