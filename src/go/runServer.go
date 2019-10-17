package main

import (
	"encoding/json"
	"fmt"
	"net"
	"orbit-messenger/src/go/db"
)

// TcpData is used to validate the user and to hold an action
type TcpData struct {
	Username string `json:"username"`
	Password string `json:"password"`
	Action   string `json:"action"`
}

type MessageData struct {
	Message string `json:"message"`
}

// OutputMessage is used to send a status to the user
type OutputMessage struct {
	OutputType string `json:"type"`
	Message    string `json:"message"`
}

func main() {
	fmt.Println("Starting Server")
	ln, err := net.Listen("tcp", ":3000")
	if err != nil {
		panic(err)
	}
	for {
		// accepts tcp connection
		conn, err := ln.Accept()
		fmt.Println("Connected with " + conn.RemoteAddr().String())
		if err != nil {
			fmt.Println(err)
		}
		go handleConnection(conn)
	}
}

// handles a connection
func handleConnection(conn net.Conn) {
	for {
		dbConn := db.CreateDatabaseConnection()
		handleAction(conn, dbConn)
	}
}

// handleAction handles all the tcp actions that the client will use
func handleAction(conn net.Conn, dbConn db.DatabaseConnection) {
	var tcpData TcpData
	decoder := json.NewDecoder(conn) // used to get json data
	encoder := json.NewEncoder(conn) // used to send json data

	// gets the username, password, and action from the tcp connection
	err := decoder.Decode(&tcpData)
	if err != nil {
		fmt.Println(err.Error())
		return
	}

	// Validate the username and password
	if !dbConn.VerifyPasswordByUsername(tcpData.Username, tcpData.Password) {
		encoder.Encode(createErrorMessage("Couldn't validate user!"))
		return
	}

	// handles all the actions
	switch tcpData.Action {
	case "get_messages":
		messages, err := dbConn.GetAllMessages()
		if err != nil {
			encoder.Encode(createErrorMessage(err.Error()))
			return
		} else {
			encoder.Encode(messages)
		}

	case "add_message":
		var message db.Message
		err = decoder.Decode(&message)
		dbConn.AddMessage(message, tcpData.Username)
		if err != nil {
			encoder.Encode(createErrorMessage(
				"Couldn't add message to the database: " + err.Error()))
		} else {
			encoder.Encode(createSuccessMessage)
		}
	default:
		encoder.Encode(createErrorMessage("Please provide valid input"))
	}
}

// createSuccessMessage will create a error message with the message provided
func createErrorMessage(message string) OutputMessage {
	return OutputMessage{"error", message}
}

// createSuccessMessage will create a status message with the message provided
func createStatusMessage(message string) OutputMessage {
	return OutputMessage{"status", message}
}

// createSuccessMessage will create a success message
func createSuccessMessage() OutputMessage {
	return OutputMessage{"status", "success"}
}
