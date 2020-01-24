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

func UserInterfaceEquals(a []db.User, b []db.User) bool {
	if len(a) != len(b) {
		return false
	}
	sort.Slice(a, func(i, j int) bool {
		return a[i].Username < a[j].Username
	})
	sort.Slice(b, func(i, j int) bool {
		return b[i].Username < b[j].Username
	})
	for i := 0; i < len(a); i++ {
		if a[i].Username != b[i].Username {
			return false
		}
	}
	return true
}

func (rc *RouteController) UpdateHandler(wsConn *websocket.Conn, state *State) {

	ticker := time.NewTicker(15 * time.Second)
	defer ticker.Stop()

	serverActionLen := rc.serverActions.ActionCount

	// used to keep the client updated with all the users and their status.
	allUsers := rc.getAllUsers()

	// waits for the user to login
	for !state.LoggedIn {
		time.Sleep(tick_speed)
	}

	for state.LoggedIn {
		start := time.Now()
		// updates the client with the current users in that chatroom
		allUsers = rc.getAllUsers()
		if !UserInterfaceEquals(allUsers.AllUsers, state.AllUsers) {
			state.AllUsers = rc.getAllUsers().AllUsers
			writeErr := wsConn.WriteJSON(allUsers)
			if writeErr != nil {
				glog.Error(writeErr.Error())
			}
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
		}

		messages := rc.getNewMessagesForClient(&state.LastMessageId, &state.Chatroom, &state.Users, &state.MessageLimit)
		if len(messages.Messages) > 0 {
			writeErr := wsConn.WriteJSON(messages)
			//glog.Infof("sending: %v", messages)
			if writeErr != nil {
				//TODO FIX ANNOYING TLS MESSAGE
				//glog.Error(writeErr.Error())
			}
		}

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
	glog.Info("out of update handler")
}
