package routes

import (
	_ "Orbit-Messenger/src/go/db"
	"fmt"
	"github.com/gorilla/websocket"
	"log"
	"strconv"
	"time"
)

// handles all the actions from the requesting client
func (rc *RouteController) handleAction(wsConn *websocket.Conn, state *State) {
	defer log.Println("Closing connection")

	for {
		wsConn.SetReadDeadline(time.Now().Add(30 * time.Second))
		var clientData ClientData
		err := wsConn.ReadJSON(&clientData)
		if err != nil {
			return
		}
		switch clientData.Action {
		case "login":
			fmt.Println("Logging in!")
			userStatusErr := rc.dbConn.ChangeUserStatus(clientData.Username, true)
			if userStatusErr != nil {
				fmt.Println("Error changing user status: ", userStatusErr.Error())
			}
			state.LoggedIn = true
			state.Username = clientData.Username

			// sends all the messages, active users, and chatrooms to the client
			messages := rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom)

			activeUsers, err := rc.dbConn.GetUsersByStatus(true)
			if err != nil {
				log.Println(err.Error())
			}

			chatrooms, err := rc.dbConn.GetAllChatrooms()
			if err != nil {
				log.Println(err.Error())
			}

			writeErr := wsConn.WriteJSON(FullData{messages.Messages, activeUsers.ActiveUsers, chatrooms.Chatrooms})
			if writeErr != nil {
				fmt.Println("Error writing to JSON: ", writeErr.Error())
			}

		case "logout":
			fmt.Println("Closing! " + clientData.Username)
			userStatusErr := rc.dbConn.ChangeUserStatus(clientData.Username, false)
			if userStatusErr != nil {
				fmt.Println("Error changing user status: ", userStatusErr.Error())
			}
			state.LoggedIn = false
			state.LoggedOut = true
			closeErr := wsConn.Close()
			if closeErr != nil {
				fmt.Println("Error closing: ", closeErr.Error())
			}
			return

		case "add":
			err = rc.dbConn.AddMessage(clientData.Message, state.Username, state.Chatroom)
			if err != nil {
				log.Println(err.Error())
			}

		case "delete":
			log.Println("deleting message ")
			rc.deleteMessageFromClient(clientData, state.Username)

		case "chatroom":
			log.Println("changing chatroom")
			state.Chatroom = clientData.Chatroom
			writeErr := wsConn.WriteJSON(rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom))
			if writeErr != nil {
				fmt.Println("Error writing to JSON: ", writeErr.Error())
			}

		default:
			err := wsConn.WriteMessage(websocket.PongMessage, []byte("pong"))
			if err != nil {
				fmt.Println("Error writing message: ", err.Error())
			}
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
