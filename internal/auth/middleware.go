package auth

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"time"
)

const sessionCookie = "pbst_session"

// SessionAuth validates the session cookie and returns JSON 401 if invalid.
// Used for /api/v1/* routes.
func SessionAuth(store *Store) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			cookie, err := r.Cookie(sessionCookie)
			if err != nil || !store.IsValid(cookie.Value) {
				jsonError(w, http.StatusUnauthorized, "Unauthorized")
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

// SessionAuthView validates the session cookie and redirects to /login.html if invalid.
// Used for view routes.
func SessionAuthView(store *Store) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			cookie, err := r.Cookie(sessionCookie)
			if err != nil || !store.IsValid(cookie.Value) {
				http.Redirect(w, r, "/login.html", http.StatusFound)
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

// RequestLogger logs method, path, and duration for every request.
func RequestLogger(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()
		next.ServeHTTP(w, r)
		slog.Info("request", "method", r.Method, "path", r.URL.Path, "duration", time.Since(start))
	})
}

func jsonOK(w http.ResponseWriter, data any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	if data != nil {
		if err := json.NewEncoder(w).Encode(data); err != nil {
			slog.Error("response encode failed", "error", err)
		}
	}
}

func jsonError(w http.ResponseWriter, status int, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(map[string]string{"error": message}); err != nil {
		slog.Error("response encode failed", "error", err)
	}
}
