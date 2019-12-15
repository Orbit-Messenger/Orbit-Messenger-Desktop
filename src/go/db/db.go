package db

import (
	"Orbit-Messenger/src/go/utils"
	"context"
	"encoding/json"
	"fmt"
	"github.com/golang/glog"
	"github.com/jackc/pgx/pgxpool"
	"io/ioutil"
)

// DATABASE_SETTINGS_FILE is the file that will be read to make a postgres url
const DATABASE_SETTINGS_FILE = "database_settings.json"

// DatabaseConnection holds all the important data for the postgres connection
type DatabaseConnection struct {
	conn         *pgxpool.Pool
	Username     string `json:"username"`
	Password     string `json:"password"`
	Port         string `json:"port"`
	DatabaseName string `json:"databaseName"`
	url          string
}

// reads the database settings file or it will ask the user for the information to create one
func (dbConn *DatabaseConnection) readOrCreateDatabaseSettingsFile() {
	fileData, err := ioutil.ReadFile(DATABASE_SETTINGS_FILE)
	if err != nil {
		glog.Error("Couldn't find database settings file! Creating a new one...")

		// The user will enter in all the database settings data
		dbConn.writeDatabaseSettingsFile()
		return
	}

	// Reads the database file into the DatabaseConnection
	err = json.Unmarshal(fileData, &dbConn)
	if err != nil {
		glog.Fatal("couldn't parse json data: " + err.Error())
	}
}

// Gets all the data from the user and creates a settings file for the database
func (dbConn *DatabaseConnection) writeDatabaseSettingsFile() {
	dbConn.Username = utils.GetUserInput("Please enter username: ")
	dbConn.Password = utils.GetUserInput("Please enter password: ")
	dbConn.Port = utils.GetUserInput("Please enter port: ")
	dbConn.DatabaseName = utils.GetUserInput("Please enter database name: ")
	dbConn.createDatabaseUrl()

	// Writes the jsonData to a file
	jsonData, err := json.Marshal(dbConn)
	if err != nil {
		glog.Fatal("Couldn't create json data for " + DATABASE_SETTINGS_FILE)
	}
	err = ioutil.WriteFile(
		DATABASE_SETTINGS_FILE,
		jsonData,
		0755)
	if err != nil {
		glog.Fatal("couldn't write file: " + err.Error())
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
	conn, err := pgxpool.Connect(context.Background(), dbConn.url)
	if err != nil {
		glog.Fatal("couldn't create database connection: " + err.Error())
	}
	dbConn.conn = conn
	return dbConn
}
