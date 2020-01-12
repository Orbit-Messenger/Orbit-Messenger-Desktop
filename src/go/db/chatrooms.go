package db

import (
	"context"
	"github.com/golang/glog"
)

const (
	create_chatroom   = "INSERT INTO chatrooms VALUES(DEFAULT, $1);"
	get_all_chatrooms = "SELECT * FROM chatrooms ORDER BY name ASC;"
	chatroom_exists   = "SELECT EXISTS(SELECT name FROM chatrooms WHERE name = $1);"
	get_name_from_id  = "SELECT name FROM chatrooms WHERE id = $1;"
	get_id_from_name  = "SELECT id FROM chatrooms WHERE name = $1;"
)

type Chatroom struct {
	Id   int64  `json:"id"`
	Name string `json:"name"`
}

type Chatrooms struct {
	Chatrooms []Chatroom `json:"chatrooms"`
}

func (dbConn DatabaseConnection) CreateChatroom(name string) error {
	_, err := dbConn.conn.Exec(context.Background(), create_chatroom, name)
	return err
}

func (dbConn DatabaseConnection) GetNameFromChatroomId(id int64) string {
	var name string
	_ = dbConn.conn.QueryRow(context.Background(), get_name_from_id, id).Scan(&name)
	return name
}

func (dbConn DatabaseConnection) GetIdFromChatroomName(name string) int64 {
	var id int64
	err := dbConn.conn.QueryRow(context.Background(), get_id_from_name, name).Scan(&id)
	if err != nil {
		glog.Error(err)
	}
	return id
}

func (dbConn DatabaseConnection) GetAllChatrooms() (Chatrooms, error) {
	var chatrooms Chatrooms
	rows, err := dbConn.conn.Query(context.Background(), get_all_chatrooms)
	if err != nil {
		return chatrooms, err
	}
	for rows.Next() {
		var chatroom Chatroom
		err = rows.Scan(&chatroom.Id, &chatroom.Name)
		if err != nil {
			return chatrooms, err
		}
		chatrooms.Chatrooms = append(chatrooms.Chatrooms, chatroom)
	}
	return chatrooms, nil
}

func (dbConn DatabaseConnection) CheckIfChatroomExists(name string) bool {
	row := dbConn.conn.QueryRow(context.Background(), chatroom_exists, name)
	var exists bool
	_ = row.Scan(&exists)
	return exists
}
