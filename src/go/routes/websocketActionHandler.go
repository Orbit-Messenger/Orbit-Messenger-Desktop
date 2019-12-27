package routes

import (
	_ "Orbit-Messenger/src/go/db"
	"fmt"
	"github.com/golang/glog"
	"github.com/gorilla/websocket"
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
				glog.Error("Error changing user status: ", userStatusErr.Error())
			}
			state.LoggedIn = false
			wsConn.Close()
			glog.Info("closing connection")
		}
	}()

	for {
		var clientData ClientData
		wsConn.SetReadDeadline(time.Now().Add(60 * time.Second))
		// When the client sends us a pong, we reset the timer!
		wsConn.SetPongHandler(func(string) error {
			wsConn.SetReadDeadline(time.Now().Add(60 * time.Second))
			return nil
		})
		err := wsConn.ReadJSON(&clientData)
		if err != nil {
			// There shouldn't be any more errors since we're handling the pong messages above. If so, lets close.
			glog.Error(err.Error())
			// This should log out a user if they get disconnected.
			userStatusErr := rc.dbConn.ChangeUserStatus(state.Username, false)
			if userStatusErr != nil {
				glog.Error(userStatusErr.Error())
			}
			wsConn.Close()
			glog.Info("closing connection")
		}

		switch clientData.Action {
		case "login":
			glog.Infof("Logging in: %v", clientData.Username)

			// Sets our message limit!
			state.MessageLimit = int64(clientData.Properties["messageNumber"].(float64))

			// changes the users online status to logged in
			userStatusErr := rc.dbConn.ChangeUserStatus(clientData.Username, true)
			if userStatusErr != nil {
				glog.Error("Error changing user status: ", userStatusErr.Error())
			}
			state.Username = clientData.Username

			// Update the new room in the DB
			userRoomErr := rc.dbConn.ChangeUserRoom(clientData.Username, state.Chatroom)
			if userRoomErr != nil {
				glog.Error("Error changing user room: ", userRoomErr.Error())
			}

			// Write user logged in!
			err = rc.dbConn.AddMessage("User joined room: "+state.Username, "admin", state.Chatroom)
			if err != nil {
				glog.Error(err.Error())
			}

			// sends all the messages, active users, and chatrooms to the client
			messages := rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom, &state.MessageLimit)

			activeUsers, err := rc.dbConn.GetUsersByStatus(true, state.Chatroom)
			if err != nil {
				glog.Error(err.Error())
			}

			chatrooms, err := rc.dbConn.GetAllChatrooms()
			if err != nil {
				glog.Error(err.Error())
			}

			writeErr := wsConn.WriteJSON(FullData{messages.Messages, activeUsers.ActiveUsers, chatrooms.Chatrooms})
			if writeErr != nil {
				glog.Error("Error writing to JSON: ", writeErr.Error())
			}
			state.ActiveUsers = activeUsers.ActiveUsers
			state.LoggedIn = true

		case "logout":
			glog.Infof("user logged out: %v ", clientData.Username)
			userStatusErr := rc.dbConn.ChangeUserStatus(clientData.Username, false)
			if userStatusErr != nil {
				fmt.Println("Error changing user status: ", userStatusErr.Error())
			}

			// Write user logged out!
			err = rc.dbConn.AddMessage("User left room: "+state.Username, "admin", state.Chatroom)
			if err != nil {
				glog.Error(err.Error())
			}

			closeErr := wsConn.Close()
			if closeErr != nil {
				glog.Error("Error closing: ", closeErr.Error())
			}
			state.LoggedIn = false
			state.LoggedOut = true
			return

		case "add":
			glog.Info("adding message")
			err = rc.dbConn.AddMessage(clientData.Message, state.Username, state.Chatroom)
			if err != nil {
				glog.Error(err.Error())
			}

		case "delete":
			glog.Info("deleting message")
			rc.deleteMessageFromClient(clientData, state.Username)

		case "chatroom":
			glog.Info("changing chatroom")
			// Write user left room!
			err = rc.dbConn.AddMessage("User left room: "+state.Username, "admin", state.Chatroom)
			if err != nil {
				glog.Error(err.Error())
			}
			// Update state to the new room
			state.Chatroom = clientData.Chatroom
			// Write user joined room!
			err = rc.dbConn.AddMessage("User joined room: "+state.Username, "admin", state.Chatroom)
			if err != nil {
				glog.Error(err.Error())
			}

			// Update the new room in the DB
			userRoomErr := rc.dbConn.ChangeUserRoom(clientData.Username, state.Chatroom)
			if userRoomErr != nil {
				glog.Error("Error changing user room: ", userRoomErr.Error())
			}

			// Set the lastMessageId to 0 so on next update it will get all the messages.
			state.LastMessageId = 0

		case "properties":
			glog.Info("changing properties")
			// Update message limit
			state.MessageLimit = int64(clientData.Properties["messageNumber"].(float64))

			state.LastMessageId = 0

		default:
			// No need to ping here since pings are being handled via Update Handler.
			fmt.Println("Didn't recognize it.")
		}
	}
}

// Gets all the messages for the client
func (rc RouteController) deleteMessageFromClient(clientData ClientData, username string) {
	messageId, err := strconv.ParseInt(clientData.Message, 10, 64)
	if err != nil {
		glog.Error(err.Error())
		return
	}
	userOfTheMessage := rc.dbConn.GetUsernameFromMessageId(messageId)
	if userOfTheMessage == username {
		rc.serverActions.AddDeleteAction(messageId)
		err = rc.dbConn.DeleteMessageById(messageId)
	}
}
