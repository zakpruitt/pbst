package auth

import (
	"crypto/rand"
	"encoding/base64"
	"sync"
	"time"
)

type Store struct {
	mu       sync.RWMutex
	sessions map[string]time.Time
}

func NewStore() *Store {
	return &Store{sessions: make(map[string]time.Time)}
}

func (s *Store) Create(duration time.Duration) (string, error) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", err
	}
	token := base64.URLEncoding.EncodeToString(b)

	s.mu.Lock()
	s.sessions[token] = time.Now().Add(duration)
	s.mu.Unlock()

	return token, nil
}

func (s *Store) IsValid(token string) bool {
	s.mu.RLock()
	expires, exists := s.sessions[token]
	s.mu.RUnlock()

	if !exists {
		return false
	}
	if time.Now().After(expires) {
		s.mu.Lock()
		delete(s.sessions, token)
		s.mu.Unlock()
		return false
	}
	return true
}

func (s *Store) Delete(token string) {
	s.mu.Lock()
	delete(s.sessions, token)
	s.mu.Unlock()
}
