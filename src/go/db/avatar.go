package db

import (
	"context"
	"fmt"
	"github.com/golang/glog"
)

const (
	ADD_AVATAR_TO_USER = "UPDATE users SET avatar_id = $1 WHERE id = $2;"
	ADD_AVATAR         = "INSERT INTO avatars VALUES(DEFAULT, DEFAULT, $1);"
	GET_NEWEST_AVATAR  = "SELECT MAX(id) FROM avatars;"
	GET_AVATAR         = "SELECT * FROM full_avatar_info WHERE username = $1"
	ALL_AVATARS        = "SELECT * FROM avatars;"
)

type Avatar struct {
	Username string
	Version  float64
	Location string
}

// Adds an avatar to a user
func (dbConn DatabaseConnection) AddAvatar(username, avatarFilePath string) error {
	userId, err := dbConn.GetUserId(username)
	if err != nil {
		return fmt.Errorf("couldn't get user ID for avatar image: %v", err)
	}
	_, err = dbConn.conn.Exec(context.Background(), ADD_AVATAR, avatarFilePath)
	if err != nil {
		glog.Error("couldn't insert avatar image path")
		return err
	}
	var avatarId int64
	err = dbConn.conn.QueryRow(context.Background(), GET_NEWEST_AVATAR).Scan(&avatarId)
	if err != nil {
		glog.Error("couldn't find the newest avatar image id")
		return err
	}
	_, err = dbConn.conn.Exec(context.Background(), ADD_AVATAR_TO_USER, avatarId, userId)
	return err
}

// Gets the username and its avatar version and location
func (dbConn DatabaseConnection) GetAvatarByUsername(username string) (Avatar, error) {
	var avatar Avatar
	err := dbConn.conn.QueryRow(context.Background(), GET_AVATAR, username).Scan(&avatar.Username, &avatar.Version, &avatar.Location)
	if err != nil {
		glog.Error(err)
		return avatar, err
	}

	return avatar, nil
}

// Gets all the usernames and their avatar locations
func (dbConn DatabaseConnection) GetAllAvatars() ([]Avatar, error) {
	var avatars []Avatar
	rows, err := dbConn.conn.Query(context.Background(), ALL_AVATARS)
	if err != nil {
		glog.Error(err)
		return avatars, err
	}

	defer rows.Close()

	for rows.Next() {
		var avatar Avatar
		err = rows.Scan(&avatar.Username, &avatar.Version, &avatar.Location)
		if err != nil {
			glog.Error(err)
			return avatars, err
		}
		avatars = append(avatars, avatar)
	}
	return avatars, nil
}
