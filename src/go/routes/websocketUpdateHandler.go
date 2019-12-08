package routes

import (
	"fmt"
	"github.com/gorilla/websocket"
	"log"
	"time"
)

const (
	tick_speed = 500 * time.Millisecond
)

func (rc *RouteController) UpdateHandler(wsConn *websocket.Conn, state *State) {
	serverActionLen := rc.serverActions.ActionCount

	// waits for the user to login
	for !state.LoggedIn {
		time.Sleep(tick_speed)
	}

	for {
		messages := rc.getNewMessagesForClient(&state.LastMessageId, &state.Chatroom)
		if len(messages.Messages) > 0 {
			writeErr := wsConn.WriteJSON(messages)
			if writeErr != nil {
				fmt.Println("1 Error writing to JSON: ", writeErr.Error())
			}
		}

		if serverActionLen != rc.serverActions.ActionCount {
			newestAction, err := rc.serverActions.GetNewestAction()
			if err != nil {
				log.Println(err)
			}
			writeErr := wsConn.WriteJSON(newestAction)
			if writeErr != nil {
				fmt.Println("2 Error writing to JSON: ", writeErr.Error())
			}
			serverActionLen = rc.serverActions.ActionCount
		}
		time.Sleep(tick_speed)
	}
}
