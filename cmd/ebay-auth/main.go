package main

import (
	"fmt"
	"log"
	"os"

	"github.com/joho/godotenv"
)

const (
	ruName = "Zak_Pruitt-ZakPruit-collec-qeyfch"
	scopes = "https://api.ebay.com/oauth/api_scope/sell.fulfillment.readonly https://api.ebay.com/oauth/api_scope/sell.finances"
)

func main() {
	godotenv.Load(".env.development")

	clientID := os.Getenv("EBAY_CLIENT_ID")
	clientSecret := os.Getenv("EBAY_CLIENT_SECRET")
	if clientID == "" || clientSecret == "" {
		log.Fatal("EBAY_CLIENT_ID and EBAY_CLIENT_SECRET must be set")
	}

	authURL := buildAuthURL(clientID)
	fmt.Println("Open this URL in your browser:")
	fmt.Println()
	fmt.Println(authURL)
	fmt.Println()
	fmt.Println("Waiting for callback on :9090...")

	refreshToken, err := waitForCallback(clientID, clientSecret)
	if err != nil {
		log.Fatalf("auth failed: %v", err)
	}

	fmt.Println()
	fmt.Println("=== Add to .env.development ===")
	fmt.Println()
	fmt.Printf("EBAY_CLIENT_ID=%s\n", clientID)
	fmt.Printf("EBAY_CLIENT_SECRET=%s\n", clientSecret)
	fmt.Printf("EBAY_REFRESH_TOKEN=%s\n", refreshToken)
	fmt.Println()
	fmt.Println("Refresh token is valid for ~18 months.")
}
