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
	base64String := strings.Replace(input, "basic ", "", 1)
	data, err := base64.StdEncoding.DecodeString(base64String)
	if err != nil {
		return output, nil
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
