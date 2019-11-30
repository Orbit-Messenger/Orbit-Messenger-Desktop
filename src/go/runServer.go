package main

import (
	"Orbit-Messenger/src/go/routes"
	"fmt"
	"github.com/gin-gonic/gin"
	"log"
	"net/http"
)

func main() {
	fmt.Println("Starting Server")
	router := gin.Default()
	routes := routes.CreateRouteController()

	// Routes
	router.GET("/", routes.WebSocket)
	router.POST("/changePassword", routes.ChangePassword)
	router.POST("/verifyUser", routes.VerifyUser)
	router.POST("/createUser", routes.CreateUser)
	router.POST("/createRoom", routes.CreateChatroom)
	//router.Run(":3000")
	log.Fatal(http.ListenAndServeTLS(":3000", "./cert.pem", "./key.pem", router))

}
