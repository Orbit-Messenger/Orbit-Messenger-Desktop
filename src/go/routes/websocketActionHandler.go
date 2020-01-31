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
func (serverState *ServerStateController) handleAction(wsConn *websocket.Conn, state *State) {
	// makes sure the connection closes smoothly
	defer serverState.handleRecovery(state, wsConn)

	for {
		clientData, err := serverState.handleReadDeadlineAndRead(state, wsConn)
		if err != nil {
			// For Debugging
			//glog.Error(err)
			return
		}
		switch clientData.Action {
		case "login":
			serverState.loginAction(clientData, state, wsConn)

		case "logout":
			serverState.logoutAction(clientData, state, wsConn)

		case "add":
			serverState.addAction(clientData, state)

		case "delete":
			serverState.deleteAction(clientData, state)

		case "chatroom":
			serverState.chatroomAction(clientData, state)

		case "properties":
			serverState.propertiesAction(clientData, state)

		default:
			// No need to ping here since pings are being handled via Update Handler.
			fmt.Println("Didn't recognize it.")
		}
	}
}

func (serverState ServerStateController) handleRecovery(state *State, wsConn *websocket.Conn) {
	if r := recover(); r != nil {
		// This should log out a user if they get disconnected.
		userStatusErr := serverState.dbConn.ChangeUserStatus(state.Username, false)
		if userStatusErr != nil {
			glog.Error("Error changing user status: ", userStatusErr.Error())
		}
		state.LoggedIn = false
		wsConn.Close()
		glog.Info("closing connection")
	}

}

func (serverState ServerStateController) handleReadDeadlineAndRead(state *State, wsConn *websocket.Conn) (ClientData, error) {
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
		//glog.Error(err.Error())
		// This should log out a user if they get disconnected.
		userStatusErr := serverState.dbConn.ChangeUserStatus(state.Username, false)
		if userStatusErr != nil {
			glog.Error(userStatusErr.Error())
		}
		wsConn.Close()
		return clientData, err
	}
	return clientData, nil
}

func (serverState ServerStateController) loginAction(clientData ClientData, state *State, wsConn *websocket.Conn) {
	glog.Infof("Logging in: %v", clientData.Username)

	if !serverState.dbConn.VerifyPasswordByUsername(clientData.Username, clientData.Password) {
		return
	}

	// Sets our message limit!
	state.MessageLimit = int64(clientData.Properties["messageNumber"].(float64))

	// changes the users online status to logged in
	userStatusErr := serverState.dbConn.ChangeUserStatus(clientData.Username, true)
	if userStatusErr != nil {
		glog.Error("Error changing user status: ", userStatusErr.Error())
	}
	state.Username = clientData.Username

	// Update the new room in the DB
	userRoomErr := serverState.dbConn.ChangeUserRoom(clientData.Username, state.Chatroom)
	if userRoomErr != nil {
		glog.Error("Error changing user room: ", userRoomErr.Error())
	}

	// Write user logged in!
	//err := serverState.dbConn.AddMessage("User joined room: "+state.Username, "admin", state.Chatroom)
	//if err != nil {
	//	glog.Error(err.Error())
	//}

	// sends all the messages, active users, and chatrooms to the client
	messages := serverState.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom, state.Users, &state.MessageLimit)

	allUsers, err := serverState.dbConn.GetAllUsers()
	if err != nil {
		glog.Error(err.Error())
	}

	chatrooms, err := serverState.dbConn.GetAllChatrooms()
	if err != nil {
		glog.Error(err.Error())
	}

	writeErr := wsConn.WriteJSON(FullData{messages.Messages, allUsers.AllUsers, chatrooms.Chatrooms})
	if writeErr != nil {
		glog.Error("Error writing to JSON: ", writeErr.Error())
	}
	state.AllUsers = allUsers.AllUsers
	state.LoggedIn = true

}

func (serverState ServerStateController) logoutAction(clientData ClientData, state *State, wsConn *websocket.Conn) {
	//glog.Infof("user logged out: %v ", clientData.Username) //DEBUG
	userStatusErr := serverState.dbConn.ChangeUserStatus(clientData.Username, false)
	if userStatusErr != nil {
		fmt.Println("Error changing user status: ", userStatusErr.Error())
	}

	// Write user logged out!
	//err := serverState.dbConn.AddMessage("User left room: "+state.Username, "admin", state.Chatroom)
	//if err != nil {
	//	glog.Error(err.Error())
	//}

	closeErr := wsConn.Close()
	if closeErr != nil {
		glog.Error("Error closing: ", closeErr.Error())
	}
	state.LoggedIn = false
	state.LoggedOut = true
	return

}

func (serverState ServerStateController) addAction(clientData ClientData, state *State) {
	glog.Info("adding message")
	var err error
	// handles adding all direct messages
	if state.Chatroom == "direct_messages" {
		err = serverState.dbConn.AddDirectMessage(clientData.Message, state.Username, state.Users[1], state.Chatroom)

		// handles normal messages
	} else {
		err = serverState.dbConn.AddMessage(clientData.Message, state.Username, state.Chatroom)
	}
	if err != nil {
		glog.Error(err.Error())
	}
}

func (serverState ServerStateController) deleteAction(clientData ClientData, state *State) {
	glog.Info("deleting message")
	serverState.deleteMessageFromClient(clientData, state.Username)
}

func (serverState ServerStateController) propertiesAction(clientData ClientData, state *State) {
	glog.Info("changing properties")
	// Update message limit
	state.MessageLimit = int64(clientData.Properties["messageNumber"].(float64))

	state.LastMessageId = 0
}

func (serverState ServerStateController) chatroomAction(clientData ClientData, state *State) {
	glog.Info("changing chatroom")
	// Update state to the new room
	//oldRoom := state.Chatroom
	state.Chatroom = clientData.Chatroom

	// Update the new room in the DB
	userRoomErr := serverState.dbConn.ChangeUserRoom(state.Username, state.Chatroom)

	if userRoomErr != nil {
		glog.Error("Error changing user room: ", userRoomErr.Error())
		return
	}

	// Set the lastMessageId to 0 so on next update it will get all the messages.
	state.LastMessageId = 0
	state.Users = clientData.Users

	if state.Chatroom != "direct_messages" {
		// Write user left room!
		//err := serverState.dbConn.AddMessage("User left room: "+state.Username, "admin", oldRoom)
		//if err != nil {
		//	glog.Error(err.Error())
		//}
		// Write user joined room!
		//err = serverState.dbConn.AddMessage("User joined room: "+state.Username, "admin", state.Chatroom)
		//if err != nil {
		//	glog.Error(err.Error())
		//}
	}
}

// Gets all the messages for the client
func (serverState ServerStateController) deleteMessageFromClient(clientData ClientData, username string) {
	messageId, err := strconv.ParseInt(clientData.Message, 10, 64)
	if err != nil {
		glog.Error(err.Error())
		return
	}
	userOfTheMessage := serverState.dbConn.GetUsernameFromMessageId(messageId)
	if userOfTheMessage == username {
		serverState.serverActions.AddDeleteAction(messageId)
		err = serverState.dbConn.DeleteMessageById(messageId)
	}
}
