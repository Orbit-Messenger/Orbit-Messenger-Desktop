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
	defer rc.handleRecovery(state, wsConn)

	for {
		clientData := rc.handleReadDeadlineAndRead(state, wsConn)
		glog.Info(clientData)

		switch clientData.Action {
		case "login":
			rc.loginAction(clientData, state, wsConn)

		case "logout":
			rc.logoutAction(clientData, state, wsConn)

		case "add":
			rc.addAction(clientData, state)

		case "delete":
			rc.deleteAction(clientData, state)

		case "chatroom":
			rc.chatroomAction(clientData, state)

		case "properties":
			rc.propertiesAction(clientData, state)

		default:
			// No need to ping here since pings are being handled via Update Handler.
			fmt.Println("Didn't recognize it.")
		}
	}
}

func (rc RouteController) handleRecovery(state *State, wsConn *websocket.Conn) {
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

}

func (rc RouteController) handleReadDeadlineAndRead(state *State, wsConn *websocket.Conn) ClientData {
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
	return clientData
}

func (rc RouteController) loginAction(clientData ClientData, state *State, wsConn *websocket.Conn) {
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
	err := rc.dbConn.AddMessage("User joined room: "+state.Username, "admin", state.Chatroom)
	if err != nil {
		glog.Error(err.Error())
	}

	// sends all the messages, active users, and chatrooms to the client
	messages := rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom, &state.Users, &state.MessageLimit)

	allUsers, err := rc.dbConn.GetAllUsers()
	if err != nil {
		glog.Error(err.Error())
	}

	chatrooms, err := rc.dbConn.GetAllChatrooms()
	if err != nil {
		glog.Error(err.Error())
	}

	writeErr := wsConn.WriteJSON(FullData{messages.Messages, allUsers, chatrooms.Chatrooms})
	if writeErr != nil {
		glog.Error("Error writing to JSON: ", writeErr.Error())
	}
	state.AllUsers = allUsers
	state.LoggedIn = true

}

func (rc RouteController) logoutAction(clientData ClientData, state *State, wsConn *websocket.Conn) {
	glog.Infof("user logged out: %v ", clientData.Username)
	userStatusErr := rc.dbConn.ChangeUserStatus(clientData.Username, false)
	if userStatusErr != nil {
		fmt.Println("Error changing user status: ", userStatusErr.Error())
	}

	// Write user logged out!
	err := rc.dbConn.AddMessage("User left room: "+state.Username, "admin", state.Chatroom)
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

}

func (rc RouteController) addAction(clientData ClientData, state *State) {
	glog.Info("adding message")
	if state.Chatroom == "direct_messages" {
		err := rc.dbConn.AddDirectMessage(clientData.Message, state.Username, state.Users[1], state.Chatroom)
		if err != nil {
			glog.Error(err.Error())
		}
	} else {
		err := rc.dbConn.AddMessage(clientData.Message, state.Username, state.Chatroom)
		if err != nil {
			glog.Error(err.Error())
		}
	}
}

func (rc RouteController) deleteAction(clientData ClientData, state *State) {
	glog.Info("deleting message")
	rc.deleteMessageFromClient(clientData, state.Username)
}

func (rc RouteController) propertiesAction(clientData ClientData, state *State) {
	glog.Info("changing properties")
	// Update message limit
	state.MessageLimit = int64(clientData.Properties["messageNumber"].(float64))

	state.LastMessageId = 0
}

func (rc RouteController) chatroomAction(clientData ClientData, state *State) {
	glog.Info("changing chatroom")
	// Update state to the new room
	oldRoom := state.Chatroom
	state.Chatroom = clientData.Chatroom

	// Update the new room in the DB
	userRoomErr := rc.dbConn.ChangeUserRoom(state.Username, state.Chatroom)

	if userRoomErr != nil {
		glog.Error("Error changing user room: ", userRoomErr.Error())
		return
	}

	// Set the lastMessageId to 0 so on next update it will get all the messages.
	state.LastMessageId = 0
	state.Users = clientData.Users

	if state.Chatroom != "direct_messages" {
		// Write user left room!
		err := rc.dbConn.AddMessage("User left room: "+state.Username, "admin", oldRoom)
		if err != nil {
			glog.Error(err.Error())
		}
		// Write user joined room!
		err = rc.dbConn.AddMessage("User joined room: "+state.Username, "admin", state.Chatroom)
		if err != nil {
			glog.Error(err.Error())
		}
	}
	glog.Infof("worked: state is: %v", state.Chatroom)
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
