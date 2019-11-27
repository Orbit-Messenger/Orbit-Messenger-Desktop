package main

import (
	"Orbit-Messenger/src/go/routes"
	"fmt"
	"github.com/gin-gonic/autotls"
	"github.com/gin-gonic/gin"
	"log"
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

	//m := autocert.Manager{
	//	Prompt:     autocert.AcceptTOS,
	//	HostPolicy: autocert.HostWhitelist(":3000", ":443"),
	//	Cache:      autocert.DirCache("/var/www/.cache"),
	//}
	//log.Fatal(autotls.RunWithManager(router, &m))

	log.Fatal(autotls.Run(router, "127.0.0.1"))
	//autotls.Run(router, ":3000")
	//router.Run(":3000")

}
