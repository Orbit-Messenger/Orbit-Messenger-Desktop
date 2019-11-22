package routes

import (
	"github.com/gorilla/websocket"
	"time"
)

func (rc *RouteController) UpdateHandler(wsConn *websocket.Conn, state *State) {
	serverActionLen := rc.serverActions.ActionCount

	// waits for the user to login
	for !state.LoggedIn {
		time.Sleep(500 * time.Millisecond)
	}

	for {
		messages := rc.getNewMessagesForClient(&state.LastMessageId, &state.Chatroom)
		if len(messages.Messages) > 0 {
			wsConn.WriteJSON(messages)
		}

		if serverActionLen != rc.serverActions.ActionCount {
			//newestAction, err := rc.serverActions.GetNewestAction
		}
		time.Sleep(500 * time.Millisecond)
	}
}
