package auth

import (
	"crypto/subtle"
	"encoding/json"
	"net/http"
	"os"
	"time"
)

type Handler struct {
	store *Store
}

func NewHandler(store *Store) *Handler {
	return &Handler{store: store}
}

type loginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

func (h *Handler) HandleLogin(w http.ResponseWriter, r *http.Request) {
	var req loginRequest
	err := json.NewDecoder(r.Body).Decode(&req)
	if err != nil {
		jsonError(w, http.StatusBadRequest, "Invalid request payload")
		return
	}

	expectedUser := os.Getenv("APP_USER")
	expectedPass := os.Getenv("APP_PASS")
	if expectedUser == "" || expectedPass == "" {
		jsonError(w, http.StatusInternalServerError, "Server misconfiguration")
		return
	}

	userMatch := subtle.ConstantTimeCompare([]byte(req.Username), []byte(expectedUser)) == 1
	passMatch := subtle.ConstantTimeCompare([]byte(req.Password), []byte(expectedPass)) == 1
	if !userMatch || !passMatch {
		jsonError(w, http.StatusUnauthorized, "Invalid credentials")
		return
	}

	token, err := h.store.Create(24 * time.Hour)
	if err != nil {
		jsonError(w, http.StatusInternalServerError, "Failed to create session")
		return
	}

	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookie,
		Value:    token,
		Path:     "/",
		Expires:  time.Now().Add(24 * time.Hour),
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteStrictMode,
	})

	jsonOK(w, map[string]string{"message": "Logged in successfully"})
}

func (h *Handler) HandleLogout(w http.ResponseWriter, r *http.Request) {
	if cookie, err := r.Cookie(sessionCookie); err == nil {
		h.store.Delete(cookie.Value)
	}

	http.SetCookie(w, &http.Cookie{
		Name:     sessionCookie,
		Value:    "",
		Path:     "/",
		Expires:  time.Unix(0, 0),
		MaxAge:   -1,
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteStrictMode,
	})

	jsonOK(w, map[string]string{"message": "Logged out successfully"})
}
