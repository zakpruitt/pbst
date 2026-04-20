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

func sessionCookie(value string, expires time.Time) *http.Cookie {
	return &http.Cookie{
		Name:     sessionCookieName,
		Value:    value,
		Path:     "/",
		Expires:  expires,
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteStrictMode,
	}
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

	http.SetCookie(w, sessionCookie(token, time.Now().Add(24*time.Hour)))
	jsonOK(w, map[string]string{"message": "Logged in successfully"})
}

func (h *Handler) HandleLogout(w http.ResponseWriter, r *http.Request) {
	if cookie, err := r.Cookie(sessionCookieName); err == nil {
		h.store.Delete(cookie.Value)
	}

	expiredCookie := sessionCookie("", time.Unix(0, 0))
	expiredCookie.MaxAge = -1
	http.SetCookie(w, expiredCookie)

	jsonOK(w, map[string]string{"message": "Logged out successfully"})
}
