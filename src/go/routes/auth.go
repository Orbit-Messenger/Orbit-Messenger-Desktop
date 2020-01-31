package routes

import (
	"encoding/base64"
	"fmt"
	"github.com/gin-gonic/gin"
	"strings"
)

// Auth is used to validate the username and password given in an http header
type Auth struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

// Creates an Auth from the given context by it's header
func (serverState ServerStateController) getAuth(c *gin.Context) (Auth, error) {
	auth, err := serverState.GetUsernameAndPasswordFromBase64(c.GetHeader("Authorization"))
	if err != nil {
		return *new(Auth), nil
	}
	return auth, nil
}

// validates the context headers basic auth against the usernames and passwords in the database
func (serverState ServerStateController) ValidateUser(c *gin.Context) bool {
	auth, err := serverState.getAuth(c)
	if err != nil {
		return false
	}
	return serverState.dbConn.VerifyPasswordByUsername(auth.Username, auth.Password)
}

// Used to get the username and password from basic auth
func (serverState ServerStateController) GetUsernameAndPasswordFromBase64(input string) (Auth, error) {
	var output Auth
	if input == "" {
		return output, fmt.Errorf("No username or password in basic auth")
	}

	// basic auth will give a string looking like basic YmFzaWMgYnJvZHk6dGVzdA==
	// this removes the basic part
	base64String := strings.Replace(input, "Basic ", "", 1)
	data, err := base64.StdEncoding.DecodeString(base64String)
	if err != nil {
		return output, err
	}
	usernameAndPassword := strings.Split(string(data), ":")
	output = Auth{usernameAndPassword[0], usernameAndPassword[1]}
	return output, nil
}
