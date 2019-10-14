package main

import (
	"fmt"
	"net"
)

func main() {
	fmt.Println("testing")
	ln, err := net.Listen("tcp", ":8080")
	if err != nil {
		panic("couldn't connect on that port")
	}
	for {
		conn, err := ln.Accept()
		if err != nil {
			fmt.Println(err)
		}
		go handleConnection(conn)
	}
}
