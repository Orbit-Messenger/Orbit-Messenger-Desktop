package routes

import (
	"Orbit-Messenger/src/go/db"
	"github.com/golang/glog"
	"github.com/gorilla/websocket"
	"log"
	"sort"
	"time"
)

const (
	tick_speed = 50 * time.Millisecond
)

func UserInterfaceEquals(group1 []db.User, group2 []db.User) bool {
	if len(group1) != len(group2) {
		return false
	}
	sort.Slice(group1, func(i, j int) bool {
		return group1[i].Username < group1[j].Username
	})
	sort.Slice(group2, func(i, j int) bool {
		return group2[i].Username < group2[j].Username
	})
	for i := 0; i < len(group1); i++ {
		if group1[i].Username != group2[i].Username {
			return false
		}
	}
	return true
}

func (serverState *ServerStateController) UpdateHandler(wsConn *websocket.Conn, state *State) {

	ticker := time.NewTicker(15 * time.Second)
	defer ticker.Stop()

	serverActionLen := serverState.serverActions.ActionCount
	currentAvatars, err := serverState.dbConn.GetAllAvatars()
	if err != nil {
		glog.Error(err)
	}

	// used to keep the client updated with all the users and their status.
	//allUsers := serverState.getAllUsers()

	// waits for the user to login
	for !state.LoggedIn {
		time.Sleep(tick_speed)
	}

	for state.LoggedIn {
		start := time.Now()

		// Updates the clients with each user in the current chatroom
		serverState.updateClientWithUsersInChatrooms(state, wsConn)

		// Updates the client by telling them to delete a message that was deleted from another client
		serverState.updateClientWithAction(&serverActionLen, state, wsConn)

		// Updates the client with the newest messages on the server
		serverState.updateClientWithNewMessages(state, wsConn)

		// Updates the profile images on a client if someone else changes theirs
		serverState.updateClientAvatars(&currentAvatars, state, wsConn)

		select {
		case <-ticker.C:
			if err := wsConn.WriteControl(websocket.PingMessage, []byte{}, time.Now().Add(10*time.Second)); err != nil {
				log.Println("ping:", err)
			}
		default:
			//fmt.Println("Not ready!", ticker.C)
		}
		end := time.Now()
		totalExecutionTime := end.Sub(start)
		// SMALL SLEEP SO THE CPU WON'T MELT.
		time.Sleep(tick_speed - totalExecutionTime)
	}
}

func (serverState ServerStateController) updateClientWithUsersInChatrooms(state *State, wsConn *websocket.Conn) {
	// updates the client with the current users in that chatroom
	allUsers := serverState.getAllUsers()
	if !UserInterfaceEquals(allUsers.AllUsers, state.AllUsers) {
		state.AllUsers = serverState.getAllUsers().AllUsers
		writeErr := wsConn.WriteJSON(allUsers)
		if writeErr != nil {
			glog.Error(writeErr.Error())
		}
	}
}

func (serverState ServerStateController) updateClientWithAction(serverActionLen *int64, state *State, wsConn *websocket.Conn) {
	if *serverActionLen != serverState.serverActions.ActionCount {
		newestAction, err := serverState.serverActions.GetNewestAction()
		if err != nil {
			glog.Error(err.Error())
		}
		writeErr := wsConn.WriteJSON(newestAction)
		if writeErr != nil {
			glog.Error(writeErr.Error())
		}
		*serverActionLen = serverState.serverActions.ActionCount
	}

}

func (serverState ServerStateController) updateClientWithNewMessages(state *State, wsConn *websocket.Conn) {
	messages := serverState.getNewMessagesForClient(&state.LastMessageId, &state.Chatroom, &state.Users, &state.MessageLimit)
	if len(messages.Messages) > 0 {
		writeErr := wsConn.WriteJSON(messages)
		//glog.Infof("sending: %v", messages)
		if writeErr != nil {
			//TODO FIX ANNOYING TLS MESSAGE
			//glog.Error(writeErr.Error())
		}
	}
}

type UpdateAvatars struct {
	UpdateAvatars string `json:"updateAvatars"`
}

func (serverState ServerStateController) updateClientAvatars(currentAvatars *[]db.Avatar, state *State, wsConn *websocket.Conn) {
	newAvatars, err := serverState.dbConn.GetAllAvatars()
	if err != nil {
		glog.Error(err)
		return
	}
	if !serverState.dbConn.CompareAvatarSlice(*currentAvatars, newAvatars) {
		*currentAvatars = newAvatars
		glog.Info("sending message")
		wsConn.WriteJSON(UpdateAvatars{UpdateAvatars: "stuff"})
	}
}
