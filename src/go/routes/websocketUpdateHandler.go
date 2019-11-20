package routes

import (
	"Orbit-Messenger/src/go/db"
	"github.com/gorilla/websocket"
	"log"
	"time"
)

func (rc *RouteController) UpdateHandler(wsConn *websocket.Conn, state *State) {
	serverActionLen := rc.serverActions.ActionCount

	// waits for the user to login
	for !state.LoggedIn {
		time.Sleep(500 * time.Millisecond)
	}

	for {
		// checks if the user has logged out
		if state.LoggedOut {
			log.Println("user logged out")
			return
		}
		if serverActionLen != rc.serverActions.ActionCount {
			clientAction, err := rc.serverActions.GetNewestAction()
			if err != nil {
				wsConn.WriteJSON(err.Error())
			} else {
				data := map[string]int64{
					clientAction.Action: clientAction.messageId,
				}
				wsConn.WriteJSON(data)
				serverActionLen = rc.serverActions.ActionCount
			}
			wsConn.WriteJSON(rc.getNewMessages(&state.LastMessageId, &state.Chatroom))
		}
		time.Sleep(500 * time.Millisecond)
	}
}

func (rc RouteController) getNewMessages(lastMessageId *int64, chatroom *string) db.Messages {
	messages, err := rc.dbConn.GetNewestMessagesFrom(*lastMessageId, *chatroom)
	if err != nil {
		log.Println(err)
		return messages
	}

	updateLastMessageId(messages.Messages, lastMessageId)
	return messages
}

// Gets all the messages for the client
func (rc RouteController) getAllMessagesForClient(lastMessageId *int64, chatroom *string) db.Messages {
	log.Println("getting All Messages")
	messages, err := rc.dbConn.GetAllMessages(*chatroom)
	if err != nil {
		log.Println(err)
		return messages
	}

	updateLastMessageId(messages.Messages, lastMessageId)
	return messages
}

func updateLastMessageId(messages []db.Message, lastMessageId *int64) {
	if len(messages) < 1 {
		return
	}
	*lastMessageId = messages[len(messages)-1].MessageId
}
