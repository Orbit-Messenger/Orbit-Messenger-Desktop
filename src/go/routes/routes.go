package routes

import (
	"Orbit-Messenger/src/go/db"
	"encoding/base64"
	"fmt"
	"github.com/gin-gonic/gin"
	"strings"
)

// RouteController controls the database for each route
type RouteController struct {
	dbConn db.DatabaseConnection
}

// Auth is used to validate the username and password given in an http header
type Auth struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// CreateRouteController will create a database connection and return a RouteController
func CreateRouteController() RouteController {
	return RouteController{
		db.CreateDatabaseConnection(),
	}
}

// Used to get the username and password from basic auth
func getUsernameAndPasswordFromBase64(input string) (Auth, error) {
	var output Auth
	if input == "" {
		return output, fmt.Errorf("No username or password in basic auth")
	}

	// basic auth will give a string looking like basic YmFzaWMgYnJvZHk6dGVzdA==
	// this removes the basic part
	base64String := strings.Replace(input, "Basic ", "", 1)
	data, err := base64.StdEncoding.DecodeString(base64String)
	fmt.Printf("\ndata: %v\n", string(data))
	if err != nil {
		return output, err
	}
	usernameAndPassword := strings.Split(string(data), ":")
	output = Auth{usernameAndPassword[0], usernameAndPassword[1]}
	return output, nil
}

// Creates an Auth from the given context by it's header
func (rc RouteController) getAuth(c *gin.Context) (Auth, error) {
	auth, err := getUsernameAndPasswordFromBase64(c.GetHeader("Authorization"))
	if err != nil {
		return *new(Auth), nil
	}
	return auth, nil
}

// validates the context headers basic auth against the usernames and passwords in the database
func (rc RouteController) validateUser(c *gin.Context) bool {
	auth, err := rc.getAuth(c)
	if err != nil {
		return false
	}
	return rc.dbConn.VerifyPasswordByUsername(auth.Username, auth.Password)
}

// Checks to see if there are new message. If so gets them!
func (rc RouteController) CheckForUpdatedMessages(c *gin.Context) {
	if rc.validateUser(c) {
		type MessageCount struct {
			MessageCount int64 `json:"messageCount"`
		}

		var info MessageCount
		c.Bind(&info)

		fmt.Println("Message Count: ", info.MessageCount)
		messages, err := rc.dbConn.CheckForUpdatedMessages(info.MessageCount)
		if err != nil {
			c.String(500, "Couldn't get messages from database")
			return
		}
		c.JSON(200, messages)
	} else {
		c.String(403, "Password or user not valid")
	}
}

// Gets message count
func (rc RouteController) GetMessageCount(c *gin.Context) {
	if rc.validateUser(c) {
		messageCount, err := rc.dbConn.GetMessageCount()
		if err != nil {
			c.String(500, "Couldn't get message count from database")
			return
		}
		c.JSON(200, messageCount)
	} else {
		c.String(403, "Password or user not valid")
	}
}

// Gets all the messages from the database
func (rc RouteController) GetAllMessages(c *gin.Context) {
	if rc.validateUser(c) {
		messages, err := rc.dbConn.GetAllMessages()
		if err != nil {
			c.String(500, "Couldn't get messages from database")
			return
		}
		c.JSON(200, messages)
	} else {
		c.String(403, "Password or user not valid")
	}
}

func (rc RouteController) AddMessage(c *gin.Context) {
	if rc.validateUser(c) {
		auth, err := rc.getAuth(c)
		var message db.Message
		c.BindJSON(&message)
		err = rc.dbConn.AddMessage(message, auth.Username)
		if err != nil {
			c.String(500, "Couldn't add message to the database")
			return
		}
		c.Status(200)
	} else {
		c.String(403, "Password or user not valid")
	}
}

func (rc RouteController) VerifyUser(c *gin.Context) {
	if rc.validateUser(c) {
		c.String(200, "User is valid")
	} else {
		c.String(403, "User is not valid")
	}
}
