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
	// Setting Gin to Release Mode!
	gin.SetMode(gin.ReleaseMode)

	// Routes has collides with an imported package name, maybe rename this?
	routes := routes.CreateRouteController()

	// Routes
	router.GET("/", routes.WebSocket)
	router.POST("/changePassword", routes.ChangePassword)
	router.POST("/verifyUser", routes.VerifyUser)
	router.POST("/createUser", routes.CreateUser)
	router.POST("/createRoom", routes.CreateChatroom)
	// This allows TLS!
	// Points to the CERT and KEY files.
	log.Fatal(http.ListenAndServeTLS(":3000", "./cert.pem", "./key.pem", router))
}
