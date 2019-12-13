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

	// makes sure the connection closes smoothly
	defer func() {
		if r := recover(); r != nil {
			// This should log out a user if they get disconnected.
			userStatusErr := rc.dbConn.ChangeUserStatus(state.Username, false)
			if userStatusErr != nil {
				fmt.Println("Error changing user status: ", userStatusErr.Error())
			}
			wsConn.Close()
			log.Println("closing connection")
		}
	}()

	for {
		var clientData ClientData
		wsConn.SetReadDeadline(time.Now().Add(60 * time.Second))
		// This handles receiving a PING by sending a PONG back to the client to keep the connection open.
		wsConn.SetPingHandler(func(string) error {
			wsConn.SetReadDeadline(time.Now().Add(60 * time.Second))
			return wsConn.WriteMessage(websocket.PongMessage, []byte("pong"))
		})
		err := wsConn.ReadJSON(&clientData)
		if err != nil {
			// There shouldn't be any more errors since we're handling the pong messages above. If so, lets close.
			fmt.Println("ERROR: ", err.Error())
			// This should log out a user if they get disconnected.
			userStatusErr := rc.dbConn.ChangeUserStatus(state.Username, false)
			if userStatusErr != nil {
				fmt.Println("Error changing user status: ", userStatusErr.Error())
			}
			wsConn.Close()
			log.Println("closing connection")
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

			// Update the new room in the DB
			userRoomErr := rc.dbConn.ChangeUserRoom(clientData.Username, "general")
			if userRoomErr != nil {
				fmt.Println("Error changing user room: ", userRoomErr.Error())
			}

			// sends all the messages, active users, and chatrooms to the client
			messages := rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom)

			activeUsers, err := rc.dbConn.GetUsersByStatus(true, state.Chatroom)
			if err != nil {
				log.Println(err.Error())
			}

			chatrooms, err := rc.dbConn.GetAllChatrooms()
			if err != nil {
				log.Println(err.Error())
			}

			// Write user logged in!
			err = rc.dbConn.AddMessage("User joined room: "+state.Username, "admin", state.Chatroom)
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

			// Write user logged out!
			err = rc.dbConn.AddMessage("User left room: "+state.Username, "admin", state.Chatroom)
			if err != nil {
				log.Println(err.Error())
			}

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
			// Write user left room!
			err = rc.dbConn.AddMessage("User left room: "+state.Username, "admin", state.Chatroom)
			if err != nil {
				log.Println(err.Error())
			}
			// Update state to the new room
			state.Chatroom = clientData.Chatroom
			// Write user joined room!
			err = rc.dbConn.AddMessage("User joined room: "+state.Username, "admin", state.Chatroom)
			if err != nil {
				log.Println(err.Error())
			}

			// Update the new room in the DB
			userRoomErr := rc.dbConn.ChangeUserRoom(clientData.Username, state.Chatroom)
			if userRoomErr != nil {
				fmt.Println("Error changing user room: ", userRoomErr.Error())
			}

			writeErr := wsConn.WriteJSON(rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom))
			if writeErr != nil {
				fmt.Println("Error writing to JSON: ", writeErr.Error())
			}

		default:
			fmt.Println("Not sure what this is. Maybe a Ping? ", clientData)
			//err := wsConn.WriteMessage(websocket.PongMessage, []byte("pong"))
			//if err != nil {
			//	fmt.Println("Error writing message: ", err.Error())
			//}
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
