package routes

import (
	"testing"
)

func TestGetUsernameAndPasswordFromBase64(t *testing.T) {
	basicAuth := "basic YnJvZHk6dGVzdA=="
	correctAuth := Auth{"brody", "test"}
	auth, err := getUsernameAndPasswordFromBase64(basicAuth)
	if auth != correctAuth {
		t.Errorf("Base64 was incorrect, Expected %v got %v", correctAuth, auth)
	}
	if err != nil {
		t.Errorf("Error converting to base64: %v", err)
	}
}
