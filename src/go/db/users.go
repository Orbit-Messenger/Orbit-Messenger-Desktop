package db

import (
	"context"
	"fmt"
)

type User struct {
	Id          int64
	Username    string
	Password    string
	Salt        string
	AccountType string
	Status      bool
}

// GetUsername gets the users password from the id
func (dbConn DatabaseConnection) GetPasswordById(id int64) (string, error) {
	var password string
	err := dbConn.conn.QueryRow(context.Background(),
		"SELECT password FROM users WHERE id = $1;", id).Scan(&password)
	if err != nil {
		return "", err
	}
	return password, nil
}

// GetUsername gets the users password from the id
func (dbConn DatabaseConnection) GetPasswordByUsername(username string) (string, error) {
	var password string
	err := dbConn.conn.QueryRow(context.Background(),
		"SELECT password FROM users WHERE username = $1;", username).Scan(&password)
	if err != nil {
		return "", err
	}
	return password, nil
}

// GetUsername gets the users username from the id
func (dbConn DatabaseConnection) GetUsername(id int64) (string, error) {
	var username string
	err := dbConn.conn.QueryRow(context.Background(),
		"SELECT username FROM users WHERE id = $1;", id).Scan(&username)
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

	columnsAffected, err := dbConn.conn.Exec(
		context.Background(),
		"UPDATE users SET status = $1 WHERE id = $2;",
		status,
		userId)
	fmt.Printf("%v columns affected", columnsAffected)
	return err

}

// Gets all the users by their status
func (dbConn DatabaseConnection) GetUsersByStatus(status bool) ([]string, error) {
	var usernames []string
	rows, err := dbConn.conn.Query(context.Background(),
		"SELECT username FROM users WHERE status = $1;", status)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	for rows.Next() {
		var username string
		err = rows.Scan(&username)
		if err != nil {
			return nil, err
		}
		usernames = append(usernames, username)
	}
	return usernames, nil
}
