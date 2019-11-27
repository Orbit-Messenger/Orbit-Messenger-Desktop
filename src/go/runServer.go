package main

import (
	"Orbit-Messenger/src/go/routes"
	"fmt"
	"github.com/gin-gonic/autotls"
	"github.com/gin-gonic/gin"
	"golang.org/x/crypto/acme/autocert"
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

	// Useing this still give me:
	// 2019/11/27 16:00:04 http: TLS handshake error from 127.0.0.1:39830: acme/autocert: missing server name
	m := autocert.Manager{
		Prompt:     autocert.AcceptTOS,
		HostPolicy: autocert.HostWhitelist(":3000", ":443"),
		Cache:      autocert.DirCache("./"),
	}
	log.Fatal(autotls.RunWithManager(router, &m))

	//log.Fatal(autotls.Run(router, "127.0.0.1"))
	//autotls.Run(router, ":3000")
	//router.Run(":3000")

	// Following this trying to get localhost certs to work
	// URL: https://github.com/gin-gonic/gin/issues/530
	//certPath := "./localhost.crt"
	//keyPath := "./localhost.key"
	//
	//router.RunTLS(":3000", certPath, keyPath)

}
