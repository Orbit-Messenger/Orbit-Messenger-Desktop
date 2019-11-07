package routes

import (
	"Orbit-Messenger/src/go/db"
	_ "fmt"
	"github.com/gorilla/websocket"
	"log"
)

// handles all the actions from the requesting client
func (rc RouteController) handleAction(conn *websocket.Conn, state *State) {
	for {
		var clientData ClientData
		err := conn.ReadJSON(&clientData)
		if err != nil {
			return
		}
		switch clientData.Action {
		case "login":
			rc.dbConn.ChangeUserStatus(clientData.Username, true)
			state.LoggedIn = true
			state.Username = clientData.Username
		case "logout":
			rc.dbConn.ChangeUserStatus(clientData.Username, false)
			state.LoggedIn = false
			state.LoggedOut = true
			conn.Close()
			return
		case "add":
			rc.addMessageFromClient(clientData, conn)
		default:
			conn.WriteMessage(websocket.PongMessage, []byte("pong"))
		}
	}
}

// Gets all the messages for the client
func (rc RouteController) addMessageFromClient(clientData ClientData, conn *websocket.Conn) {
	log.Println("Adding message")
	message := db.Message{-1, clientData.Username, clientData.Message}
	rc.dbConn.AddMessage(message, message.Username)
}
