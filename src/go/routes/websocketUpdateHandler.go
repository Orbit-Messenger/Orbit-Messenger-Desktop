package routes

import (
	"fmt"
	"github.com/golang/glog"
	"github.com/gorilla/websocket"
	"log"
	"sort"
	"time"
)

const (
	tick_speed = 500 * time.Millisecond
)

func StringArrayEquals(a []string, b []string) bool {
	if len(a) != len(b) {
		return false
	}
	sort.Strings(a)
	sort.Strings(b)
	for i := 0; i < len(a); i++ {
		if a[i] != b[i] {
			return false
		}
	}
	return true
}

func (rc *RouteController) UpdateHandler(wsConn *websocket.Conn, state *State) {

	ticker := time.NewTicker(15 * time.Second)
	defer ticker.Stop()

	serverActionLen := rc.serverActions.ActionCount

	// used to keep the client updated with how many users are in the current chatroom
	activeUsers := rc.getActiveUsersForClient(state.Chatroom)

	// waits for the user to login
	for !state.LoggedIn {
		time.Sleep(tick_speed)
	}

	for {
		messages := rc.getNewMessagesForClient(&state.LastMessageId, &state.Chatroom)
		if len(messages.Messages) > 0 {
			writeErr := wsConn.WriteJSON(messages)
			glog.Infof("sending: %v", messages)
			if writeErr != nil {
				//TODO FIX ANNOYING TLS MESSAGE
				//glog.Error(writeErr.Error())
			}
			time.Sleep(tick_speed)
		}

		//// updates the client with the current users in that chatroom
		activeUsers = rc.getActiveUsersForClient(state.Chatroom)
		if !StringArrayEquals(activeUsers.ActiveUsers, state.ActiveUsers) {
			fmt.Println("Users different: ", activeUsers)
			state.ActiveUsers = rc.getActiveUsersForClient(state.Chatroom).ActiveUsers
			writeErr := wsConn.WriteJSON(activeUsers)
			if writeErr != nil {
				glog.Error(writeErr.Error())
			}
			time.Sleep(tick_speed)
		}

		if serverActionLen != rc.serverActions.ActionCount {
			newestAction, err := rc.serverActions.GetNewestAction()
			if err != nil {
				glog.Error(err.Error())
			}
			writeErr := wsConn.WriteJSON(newestAction)
			if writeErr != nil {
				glog.Error(writeErr.Error())
			}
			serverActionLen = rc.serverActions.ActionCount
			time.Sleep(tick_speed)
		}

		select {
		case <-ticker.C:
			//fmt.Println("SENDING PING TO CLIENT: ", state.Username)
			if err := wsConn.WriteControl(websocket.PingMessage, []byte{}, time.Now().Add(10*time.Second)); err != nil {
				log.Println("ping:", err)
			}
		default:
			//fmt.Println("Not ready!", ticker.C)
		}
		time.Sleep(tick_speed)
	}
}
