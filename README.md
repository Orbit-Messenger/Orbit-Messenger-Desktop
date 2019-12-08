![Orbit Messenger logo](https://github.com/MaxwellM/Orbit-Messenger/blob/master/src/images/orbit-messenger-logo.png)

## GENERAL
This application consists of a server and client component.  
The server consists of Golang and Gin/Gorilla.  
The database consists of PostgreSQL.  
The client consists of Java11 and JavaFX.  

Orbit Messenger is a multi-threaded, TLS encrypted, websocketed application that allows multiple users to communicate via secured messaging. 

##FEATURE LIST
* Create Users
* Change User Password
* Create Rooms
* Deleting rooms
* TLS support
* Deleting messages
* Copy/Paste message
* Number of messages returned
* Window resizing
* CUSTOM CSS THEMES!

## DATABASE
* Create a database called orbitmessengerdb
run:
```
cd src
psql -U postgres -d orbitmessengerdb -f sql/createTables.sql
```

## BUILD SERVER
its possible to run the runServer binary on any linux machine, but to build type
```
cd src
./buildServer.py
cd ..
./runServer
```

## HTTP actions
to get messenges from the server you will need to connect to the server by port 3000
to test type
```
curl -X GET localhost:3000/getAllMessages -u brody:test
```
or to add a message type
```
curl -X POST localhost:3000/addMessage -u brody:test -d '{"message": "Testing"}'

```


