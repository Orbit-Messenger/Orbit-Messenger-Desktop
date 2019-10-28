package db

import (
	"Orbit-Messenger/src/go/utils"
	"context"
	"encoding/json"
	"fmt"
	"github.com/jackc/pgx"
	"io/ioutil"
)

// DATABASE_SETTINGS_FILE is the file that will be read to make a postgres url
const DATABASE_SETTINGS_FILE = "database_settings.json"

// DatabaseConnection holds all the important data for the postgres connection
type DatabaseConnection struct {
	conn         *pgx.Conn
	Username     string `json:"username"`
	Password     string `json:"password"`
	Port         string `json:"port"`
	DatabaseName string `json:"databaseName"`
	url          string
}

// Message holds all the data for a message from the database
type Message struct {
	MessageId int64  `json:"messageId"`
	Username  string `json:"username"`
	Message   string `json:"message"`
}

type User struct {
	Id          int64
	Username    string
	Password    string
	Salt        string
	AccountType string
}

// reads the database settings file or it will ask the user for the information to create one
func (dbConn *DatabaseConnection) readOrCreateDatabaseSettingsFile() {
	fileData, err := ioutil.ReadFile(DATABASE_SETTINGS_FILE)
	if err != nil {
		fmt.Println("Couldn't find database settings file! Creating a new one...")

		// The user will enter in all the database settings data
		dbConn.writeDatabaseSettingsFile()
		return
	}

	// Reads the database file into the DatabaseConnection
	err = json.Unmarshal(fileData, &dbConn)
	if err != nil {
		panic("couldn't parse json data: " + err.Error())
	}
}

// Gets all the data from the user and creates a settings file for the database
func (dbConn *DatabaseConnection) writeDatabaseSettingsFile() {
	dbConn.Username = utils.GetUserInput("Please enter username: ")
	dbConn.Password = utils.GetUserInput("Please enter password: ")
	dbConn.Port = utils.GetUserInput("Please enter port: ")
	dbConn.DatabaseName = utils.GetUserInput("Please enter database name: ")
	dbConn.createDatabaseUrl()
	fmt.Println(dbConn.url)

	// Writes the jsonData to a file
	jsonData, err := json.Marshal(dbConn)
	if err != nil {
		panic("Couldn't create json data for " + DATABASE_SETTINGS_FILE)
	}
	err = ioutil.WriteFile(
		DATABASE_SETTINGS_FILE,
		jsonData,
		0755)
	if err != nil {
		panic("couldn't write file: " + err.Error())
	}
}

// Creates the postgres url
func (dbConn *DatabaseConnection) createDatabaseUrl() {
	dbConn.url = fmt.Sprintf(
		"postgres://%v:%s@%v:%v/%v",
		dbConn.Username,
		dbConn.Password,
		"localhost",
		dbConn.Port,
		dbConn.DatabaseName)
}

// Creates the DatabaseConnection object
func CreateDatabaseConnection() DatabaseConnection {
	var dbConn DatabaseConnection
	dbConn.readOrCreateDatabaseSettingsFile()
	dbConn.createDatabaseUrl()

	// creates the connection with pgx
	conn, err := pgx.Connect(context.Background(), dbConn.url)
	if err != nil {
		panic("couldn't create datbase connection: " + err.Error())
	}
	dbConn.conn = conn
	return dbConn
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

// adds a message to the database under the username provided
func (dbConn DatabaseConnection) AddMessage(message Message, username string) error {
	fmt.Printf("The message is: %v", message.Message)
	userId, err := dbConn.GetUserId(username)
	if userId == 0 || err != nil {
		return fmt.Errorf("Couldn't find anyone with the username %v", username)
	}

	columnsAffected, err := dbConn.conn.Exec(
		context.Background(),
		"INSERT INTO messages VALUES(DEFAULT, $1, $2);",
		userId,
		message.Message)
	fmt.Printf("%v columns affected", columnsAffected)
	return err
}
