package routes

import (
	_ "Orbit-Messenger/src/go/db"
	"fmt"
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
			fmt.Println("Logging in!")
			rc.dbConn.ChangeUserStatus(clientData.Username, true)
			state.LoggedIn = true
			state.Username = clientData.Username

			// sends all the messages, active users, and chatrooms to the client
			messages := rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom)

			activeUsers, err := rc.dbConn.GetUsersByStatus(true)
			if err != nil {
				log.Println(err)
			}

			chatrooms, err := rc.dbConn.GetAllChatrooms()
			if err != nil {
				log.Println(err)
			}

			wsConn.WriteJSON(FullData{messages.Messages, activeUsers.ActiveUsers, chatrooms.Chatrooms})

		case "logout":
			fmt.Println("Closing! " + clientData.Username)
			rc.dbConn.ChangeUserStatus(clientData.Username, false)
			state.LoggedIn = false
			state.LoggedOut = true
			wsConn.Close()
			return

		case "add":
			err = rc.dbConn.AddMessage(clientData.Message, state.Username, state.Chatroom)
			if err != nil {
				log.Println(err)
			}

		case "delete":
			log.Println("deleting message ")
			rc.deleteMessageFromClient(clientData, state.Username)

		case "chatroom":
			log.Println("changing chatroom")
			state.Chatroom = clientData.Chatroom
			wsConn.WriteJSON(rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom))

		default:
			wsConn.WriteMessage(websocket.PongMessage, []byte("pong"))
		}
	}
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
		err = rc.dbConn.DeleteMessageById(messageId)
	}
}
