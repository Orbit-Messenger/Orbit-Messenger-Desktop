![Orbit Messenger logo](https://github.com/MaxwellM/Orbit-Messenger/blob/master/src/images/orbit-messenger-logo.png)

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
curl -X GET localhost:3000/getAllMessages -u brody:things
```
or to add a message type
```
curl -X POST localhost:3000/addMessage -u brody:test -d '{"message": "Testing"}'

```


