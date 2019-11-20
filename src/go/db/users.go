package db

import (
	"context"
	"fmt"
)

const (
	GET_ID_FROM_USERNAME       = "SELECT id FROM users WHERE username = $1;"
	GET_PASSWORD_FROM_ID       = "SELECT password FROM users WHERE id = $1;"
	GET_PASSWORD_FROM_USERNAME = "SELECT password FROM users WHERE username = $1;"
	GET_USERNAME_FROM_ID       = "SELECT username FROM users WHERE id = $1;"
	UPDATE_USER_STATUS         = "UPDATE users SET status = $1 WHERE id = $2;"
	GET_USERNAMES_FROM_STATUS  = "SELECT username FROM users WHERE status = $1;"
	CHECK_IF_USER_EXISTS       = "SELECT EXISTS(SELECT username FROM users WHERE username = $1);"
	CREATE_USER                = "INSERT INTO users VALUES(DEFAULT, $1, $2, $2)"
)

type User struct {
	Id       int64
	Username string
	Password string
	Salt     string
	Status   bool
}

type ActiveUsers struct {
	ActiveUsers []string `json:"activeUsers"`
}

// GetUserId gets the users id from the username
func (dbConn DatabaseConnection) GetUserId(username string) (int64, error) {
	var id int64
	err := dbConn.conn.QueryRow(context.Background(), GET_ID_FROM_USERNAME, username).Scan(&id)
	if err != nil {
		return -1, err
	}
	return id, nil
}

// GetUsername gets the users password from the id
func (dbConn DatabaseConnection) GetPasswordById(id int64) (string, error) {
	var password string
	err := dbConn.conn.QueryRow(context.Background(), GET_PASSWORD_FROM_ID, id).Scan(&password)
	if err != nil {
		return "", err
	}
	return password, nil
}

// GetUsername gets the users password from the id
func (dbConn DatabaseConnection) GetPasswordByUsername(username string) (string, error) {
	var password string
	err := dbConn.conn.QueryRow(context.Background(), GET_PASSWORD_FROM_USERNAME, username).Scan(&password)
	if err != nil {
		return "", err
	}
	return password, nil
}

// GetUsername gets the users username from the id
func (dbConn DatabaseConnection) GetUsername(id int64) (string, error) {
	var username string
	err := dbConn.conn.QueryRow(context.Background(),
		GET_USERNAME_FROM_ID, id).Scan(&username)
	if err != nil {
		return "", err
	}
	return username, nil
}

// Verifys the password of a user
func (dbConn DatabaseConnection) VerifyPasswordByUsername(username, password string) bool {
	realPassword, err := dbConn.GetPasswordByUsername(username)
	if err != nil || realPassword == "" {
		return false // couldn't find the user?
	}
	return realPassword == password
}

// Changes the users status
func (dbConn DatabaseConnection) ChangeUserStatus(username string, status bool) error {
	userId, err := dbConn.GetUserId(username)
	if userId == 0 || err != nil {
		return fmt.Errorf("Couldn't find anyone with the username %v", username)
	}

	_, err = dbConn.conn.Exec(context.Background(), UPDATE_USER_STATUS, status, userId)
	return err

}

// Gets all the users by their status
func (dbConn DatabaseConnection) GetUsersByStatus(status bool) (ActiveUsers, error) {
	var usernames ActiveUsers
	rows, err := dbConn.conn.Query(context.Background(), GET_USERNAMES_FROM_STATUS, status)
	if err != nil {
		return usernames, err
	}
	defer rows.Close()

	for rows.Next() {
		var username string
		err = rows.Scan(&username)
		if err != nil {
			return usernames, err
		}
		usernames.ActiveUsers = append(usernames.ActiveUsers, username)
	}
	return usernames, nil
}

// checks if a user exists
func (dbConn DatabaseConnection) CheckIfUserExists(username string) bool {
	var exists bool
	_ = dbConn.conn.QueryRow(context.Background(), CHECK_IF_USER_EXISTS, username).Scan(exists)
	return exists
}

// checks if a user exists
func (dbConn DatabaseConnection) CreateUser(username, password string) error {
	_, err := dbConn.conn.Exec(context.Background(), CREATE_USER, username, password)
	return err
}
