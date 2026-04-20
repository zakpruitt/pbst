package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
)

const (
	ebayAuthURL  = "https://auth.ebay.com/oauth2/authorize"
	ebayTokenURL = "https://api.ebay.com/identity/v1/oauth2/token"
)

func buildAuthURL(clientID string) string {
	u, _ := url.Parse(ebayAuthURL)
	q := u.Query()
	q.Set("client_id", clientID)
	q.Set("response_type", "code")
	q.Set("redirect_uri", ruName)
	q.Set("scope", scopes)
	u.RawQuery = q.Encode()
	return u.String()
}

func exchangeCode(clientID, clientSecret, code string) (string, error) {
	body := url.Values{}
	body.Set("grant_type", "authorization_code")
	body.Set("code", code)
	body.Set("redirect_uri", ruName)

	req, err := http.NewRequest(http.MethodPost, ebayTokenURL, strings.NewReader(body.Encode()))
	if err != nil {
		return "", fmt.Errorf("build request: %w", err)
	}

	creds := base64.StdEncoding.EncodeToString([]byte(clientID + ":" + clientSecret))
	req.Header.Set("Authorization", "Basic "+creds)
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("request: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("ebay returned %d", resp.StatusCode)
	}

	var result struct {
		RefreshToken string `json:"refresh_token"`
	}
	err = json.NewDecoder(resp.Body).Decode(&result)
	if err != nil {
		return "", fmt.Errorf("decode response: %w", err)
	}

	return result.RefreshToken, nil
}
