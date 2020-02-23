# Orbit Messenger
![Orbit Messenger logo](https://github.com/MaxwellM/Orbit-Messenger/blob/master/src/java/com/orbitmessenger/images/orbit-messenger-logo.png)

Orbit Messenger is a multi-threaded, TLS encrypted, websocketed JavaFX application that allows multiple users to communicate via secured messaging with a Go server and PostgreSQL database. 

## Getting Started
You'll need to pull the project onto each system that will be utilizing the project. That would include each client and the particular server to act as the host. The server doesn't need to be separate from the client.  

Make sure you have Go v1.13.8 and Postgres v12.1 or newer installed.
##### Create DB
* Create a database called orbitmessengerdb
run:
```
cd src
psql -U postgres -d orbitmessengerdb -f sql/createTables.sql
```
##### Build Server
From src run:
```
cd src; ./buildServer.py; cd ..; ./runServer
```

## GENERAL
This application consists of a server and client component.  
The server consists of Golang and Gin/Gorilla.  
The database consists of PostgreSQL.  
The client consists of Java11 and JavaFX.  

## TESTED VERSION OF LIBRARIES
Languages  
Go - v1.13.8  
Postgres - v12.1  
Java - v11.0.5  

Libraries  
JavaFX - v11.0.2  
Commons Codec - v1.11  
Commons Logging - v1.2  
ControlsFX - v11.0.0  
Gson - v2.8.5  
Httpasyncclient - v4.1.4  
Httpclient - v4.5.9  
Httpcore - v4.4.11  
Httpcore Nio - v.4.4.10  
Httpmime - v4.5.9  
Java Websocket - v1.4.0  
Log4j - v1.2.17  
slf4j Api - v1.7.25  
slf4j Log - v12.1.7.12  
Unirest Java - v.3.1.02

## FEATURE LIST
* Create Users
* Change User Password
* Create Rooms
* Deleting rooms
* TLS support
* Deleting messages
* Copy/Paste message
* Number of messages returned
* Group messages
* Profile pictures
* Window resizing
* CUSTOM CSS THEMES!

## TLS
WSS support requires having the keystore.jks 


