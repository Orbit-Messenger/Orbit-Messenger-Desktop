package db

import (
	"context"
	"fmt"
	"github.com/golang/glog"
	"time"
)

const (
	ADD_MESSAGE                  = "INSERT INTO messages VALUES(DEFAULT, $1, $2, $3);"
	GET_ALL_MESSAGES             = "SELECT * FROM full_messages WHERE name = $1 ORDER BY id DESC LIMIT $2;"
	GET_NEWEST_MESSAGES          = "SELECT * FROM full_messages WHERE id > $1 and name = $2 ORDER BY id DESC LIMIT $3;"
	GET_MESSAGE_COUNT            = "SELECT count(id) FROM full_messages WHERE name = $1;"
	GET_USERNAME_FROM_MESSAGE_ID = "SELECT users.username FROM messages INNER JOIN users ON users.id = messages.user_id WHERE messages.id = $1;"
	DELETE_MESSAGE               = "DELETE FROM messages WHERE id = $1;"
)

// Message holds all the data for a message from the database
type Message struct {
	MessageId int64     `json:"messageId"`
	Username  string    `json:"username"`
	Chatroom  string    `json:"chatroom"`
	Message   string    `json:"message"`
	Timestamp time.Time `json:"timestamp"`
}

type Messages struct {
	Messages []Message `json:"messages"`
}

// adds a message to the database under the username provided
func (dbConn DatabaseConnection) AddMessage(message, username, chatroomName string) error {
	userId, err := dbConn.GetUserId(username)
	chatroomId := dbConn.GetIdFromChatroomName(chatroomName)
	if userId == 0 || err != nil {
		return fmt.Errorf("Couldn't find anyone with the username %v", username)
	}

	_, err = dbConn.conn.Exec(context.Background(), ADD_MESSAGE, userId, chatroomId, message)
	return err
}

// GetAllMessages returns an array of Message types containing all the messages from the database
func (dbConn DatabaseConnection) GetAllMessages(chatroom string, messageLimit int64) (Messages, error) {
	var messages Messages
	var count int
	rows, err := dbConn.conn.Query(context.Background(), GET_ALL_MESSAGES, chatroom, messageLimit)
	if err != nil {
		return messages, err
	}
	defer rows.Close()

	for rows.Next() {
		var message Message
		err = rows.Scan(&message.MessageId, &message.Username, &message.Chatroom, &message.Message, &message.Timestamp)
		if err != nil {
			return messages, err
		}
		messages.Messages = append(messages.Messages, message)
		count++
	}

	fmt.Println("COUNT: ", count)

	return messages, nil
}

func (dbConn DatabaseConnection) GetNewestMessagesFrom(messageId int64, chatroom string, messageLimit int64) (Messages, error) {
	var messages Messages
	rows, err := dbConn.conn.Query(context.Background(), GET_NEWEST_MESSAGES, messageId, chatroom, messageLimit)
	if err != nil {
		return messages, err
	}
	defer rows.Close()

	for rows.Next() {
		var message Message
		err = rows.Scan(&message.MessageId, &message.Username, &message.Chatroom, &message.Message, &message.Timestamp)
		if err != nil {
			return messages, err
		}
		messages.Messages = append(messages.Messages, message)
	}
	return messages, nil
}

// GetMessageCount returns the message count from the database
func (dbConn DatabaseConnection) GetMessageCount(chatroom string) int64 {
	var count int64
	err := dbConn.conn.QueryRow(context.Background(), GET_MESSAGE_COUNT, chatroom).Scan(&count)
	if err != nil {
		glog.Error(err)
	}
	return count
}

// deletes the message from the db by using its id
func (dbConn DatabaseConnection) DeleteMessageById(messageId int64) error {
	_, err := dbConn.conn.Exec(context.Background(), DELETE_MESSAGE, messageId)
	return err
}

// GetUsernameFromMessageId gets the username from a message id
func (dbConn DatabaseConnection) GetUsernameFromMessageId(messageId int64) string {
	var username string
	row := dbConn.conn.QueryRow(context.Background(), GET_USERNAME_FROM_MESSAGE_ID, messageId)
	_ = row.Scan(&username)
	return username
}
