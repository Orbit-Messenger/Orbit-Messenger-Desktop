package db

import (
	_ "Orbit-Messenger/src/go/utils"
	"context"
	_ "github.com/jackc/pgx"
)

type Chatroom struct {
	Id   int64  `json:"id"`
	Name string `json:"name"`
}

func (dbConn DatabaseConnection) CreateChatroom(name string) error {
	row := dbConn.conn.QueryRow(context.Background(), "INSERT INTO chatrooms VALUES(DEFAULT, $1);", name)
	err := row.Scan()
	return err
}

func (dbConn DatabaseConnection) GetAllChatrooms() ([]Chatroom, error) {
	var chatrooms []Chatroom
	rows, err := dbConn.conn.Query(context.Background(), "SELECT name FROM chatrooms;")
	if err != nil {
		return chatrooms, err
	}
	for rows.Next() {
		var chatroom Chatroom
		err = rows.Scan(&chatroom.Id, &chatroom.Name)
		if err != nil {
			return chatrooms, err
		}
		chatrooms = append(chatrooms, chatroom)
	}
	return chatrooms, nil
}

func (dbConn DatabaseConnection) CheckIfChatroomExists(name string) bool {
	row := dbConn.conn.QueryRow(context.Background(), "SELECT name FROM chatrooms WHERE name = $1;", name)
	var dbName string
	_ = row.Scan(&dbName)
	if dbName == "" {
		return false
	}
	return true
}
