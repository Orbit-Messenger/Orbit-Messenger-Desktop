package db

import (
	_ "Orbit-Messenger/src/go/utils"
	"context"
	"fmt"
	_ "github.com/jackc/pgx"
)

// Message holds all the data for a message from the database
type Message struct {
	MessageId int64  `json:"messageId"`
	Username  string `json:"username"`
	Message   string `json:"message"`
}

// adds a message to the database under the username provided
func (dbConn DatabaseConnection) AddMessage(message Message, username string) error {
	userId, err := dbConn.GetUserId(username)
	if userId == 0 || err != nil {
		return fmt.Errorf("Couldn't find anyone with the username %v", username)
	}

	_, err = dbConn.conn.Exec(
		context.Background(),
		"INSERT INTO messages VALUES(DEFAULT, $1, $2);",
		userId,
		message.Message)
	return err
}

func (dbConn DatabaseConnection) CheckForUpdatedMessages(messageCount int64) ([]Message, error) {

	// First lets check if the messages are different than they were the last time we checked. If so, end.
	returnedMessageCount, messageErr := dbConn.GetMessageCount()
	if messageErr != nil {
		return nil, messageErr
	}

	if messageCount != returnedMessageCount {
		// Update the messages API
		messages, err := dbConn.GetAllMessages()
		if err != nil {
			return nil, err
		}
		return messages, nil
	} else {
		return nil, nil
	}
}

// GetAllMessages returns an array of Message types containing all the messages from the database
func (dbConn DatabaseConnection) GetAllMessages() ([]Message, error) {
	var messages []Message
	rows, err := dbConn.conn.Query(context.Background(),
		"SELECT messages.id, users.username, messages.message FROM messages INNER JOIN users ON users.id = messages.user_id;")
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	for rows.Next() {
		var message Message
		err = rows.Scan(&message.MessageId, &message.Username, &message.Message)
		if err != nil {
			return nil, err
		}
		messages = append(messages, message)
	}
	return messages, nil
}

func (dbConn DatabaseConnection) GetNewestMessagesFrom(messageId int64) ([]Message, error) {
	var messages []Message
	rows, err := dbConn.conn.Query(context.Background(),
		"SELECT messages.id, users.username, messages.message FROM messages INNER JOIN users ON users.id = messages.user_id WHERE messages.id > $1 LIMIT 100;", messageId)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	for rows.Next() {
		var message Message
		err = rows.Scan(&message.MessageId, &message.Username, &message.Message)
		if err != nil {
			return nil, err
		}
		messages = append(messages, message)
	}
	return messages, nil
}

// GetMessageCount returns the message count from the database
func (dbConn DatabaseConnection) GetMessageCount() (int64, error) {
	var count int64
	err := dbConn.conn.QueryRow(context.Background(),
		"SELECT count(id) FROM messages;").Scan(&count)
	if err != nil {
		return -1, err
	}
	return count, nil
}

// GetUserId gets the users id from the username
func (dbConn DatabaseConnection) GetUserId(username string) (int64, error) {
	var id int64
	err := dbConn.conn.QueryRow(context.Background(),
		"SELECT id FROM users WHERE username = $1;", username).Scan(&id)
	if err != nil {
		return -1, err
	}
	return id, nil
}

// deletes the message from the db by using its id
func (dbConn DatabaseConnection) DeleteMessageById(messageId int64) bool {
	row := dbConn.conn.QueryRow(context.Background(),
		"DELETE FROM messages WHERE id = $1;", messageId)

	err := row.Scan()
	if err != nil {
		return false
	}
	return true
}

// GetUsernameFromMessageId gets the username from a message id
func (dbConn DatabaseConnection) GetUsernameFromMessageId(messageId int64) (string, error) {
	var username string
	row := dbConn.conn.QueryRow(context.Background(),
		"SELECT users.username FROM messages INNER JOIN users ON users.id = messages.user_id;")
	err := row.Scan(&username)
	if err != nil {
		return "", err
	}
	return username, nil
}
