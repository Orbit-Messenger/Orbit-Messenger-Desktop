package routes

import (
	"fmt"
	"github.com/golang/glog"
	"github.com/gorilla/websocket"
	"time"
)

const (
	tick_speed = 500 * time.Millisecond
)

func (rc *RouteController) UpdateHandler(wsConn *websocket.Conn, state *State) {
	serverActionLen := rc.serverActions.ActionCount
	fmt.Println("Action Len: ", serverActionLen)

	// waits for the user to login
	for !state.LoggedIn {
		time.Sleep(tick_speed)
	}

	writeErr := wsConn.WriteJSON(rc.getAllMessagesForClient(&state.LastMessageId, &state.Chatroom))
	if writeErr != nil {
		glog.Error("Error writing to JSON: ", writeErr.Error())
	}

	for {
		start := time.Now()
		messages := rc.getNewMessagesForClient(&state.LastMessageId, &state.Chatroom)
		if len(messages.Messages) > 0 {
			writeErr := wsConn.WriteJSON(messages)
			if writeErr != nil {
				glog.Error(writeErr.Error())
			}
		}

		activeUsers := rc.getActiveUsersForClient(state.Chatroom)
		if len(activeUsers.ActiveUsers) > 0 {
			writeErr := wsConn.WriteJSON(activeUsers)
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
		end := time.Now()
		totalExecutionTime := end.Sub(start)
		time.Sleep(tick_speed - totalExecutionTime)
	}

}
