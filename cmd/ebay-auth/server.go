package main

import (
	"context"
	"fmt"
	"net/http"
)

func waitForCallback(clientID, clientSecret string) (string, error) {
	done := make(chan string, 1)
	errCh := make(chan error, 1)

	mux := http.NewServeMux()
	srv := &http.Server{
		Addr:    ":9090",
		Handler: mux,
	}

	mux.HandleFunc("/callback", func(w http.ResponseWriter, r *http.Request) {
		code := r.URL.Query().Get("code")
		if code == "" {
			http.Error(w, "missing code", http.StatusBadRequest)
			errCh <- fmt.Errorf("no code in callback")
			return
		}

		refreshToken, err := exchangeCode(clientID, clientSecret, code)
		if err != nil {
			http.Error(w, "token exchange failed", http.StatusInternalServerError)
			errCh <- err
			return
		}

		fmt.Fprintln(w, "Success! Check your terminal for the refresh token.")
		done <- refreshToken
	})

	go func() {
		err := srv.ListenAndServe()
		if err != nil && err != http.ErrServerClosed {
			errCh <- fmt.Errorf("server: %w", err)
		}
	}()

	var result string
	select {
	case result = <-done:
	case err := <-errCh:
		return "", err
	}

	srv.Shutdown(context.Background())
	return result, nil
}
