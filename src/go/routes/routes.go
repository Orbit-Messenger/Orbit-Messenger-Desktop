package routes

import (
	"Orbit-Messenger/src/go/db"
	"fmt"
	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
)

// RouteController controls the database for each route
type RouteController struct {
	dbConn db.DatabaseConnection
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
}

type ClientData struct {
	Message       string                 `json:"message"`
	Username      string                 `json:"username"`
	Action        string                 `json:"action"`
	LastMessageId int64                  `json:"lastMessageId"`
	Properties    map[string]interface{} `json:"properties"`
}

type ServerResponse struct {
	ActiveUsers []string     `json:"activeUsers"`
	Messages    []db.Message `json:"messages"`
	Errors      string       `json:"errors"`
}

type State struct {
	LastMessageId int64
	Username      string
	LoggedIn      bool
	LoggedOut     bool
}

// CreateRouteController will create a database connection and return a RouteController
func CreateRouteController() RouteController {
	return RouteController{
		db.CreateDatabaseConnection(),
	}
}

func (rc RouteController) WebSocket(c *gin.Context) {
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		fmt.Println(err)
	}
	var state State

	go rc.handleAction(conn, &state)
	go rc.UpdateHandler(conn, &state)
}
