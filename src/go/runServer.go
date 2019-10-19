package main

import (
	"fmt"
	"github.com/gin-gonic/gin"
	"orbit-messenger/src/go/routes"
)

func main() {
	fmt.Println("Starting Server")
	router := gin.Default()
	routes := routes.CreateRouteController()

	// Routes
	router.GET("/getAllMessages", routes.GetAllMessages)
	router.POST("/addMessage", routes.AddMessage)

	router.Run(":3000")

}
