package utils

import (
	"bufio"
	"fmt"
	"os"
	"strings"
)

// GetUserInput gets the user input from stdin
func GetUserInput(message string) string {
	fmt.Print(message)
	reader := bufio.NewReader(os.Stdin)
	userInput, _ := reader.ReadString('\n')
	userInput = strings.Trim(userInput, "\n")
	fmt.Println(userInput)
	return userInput
}
