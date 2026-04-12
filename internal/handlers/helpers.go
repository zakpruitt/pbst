package handlers

import (
	"database/sql"
	"net/http"
	"net/url"
	"strconv"
	"time"
)

// parsePathID parses an unsigned integer from a named URL path segment.
func parsePathID(r *http.Request, param string) (uint, error) {
	id, err := strconv.ParseUint(r.PathValue(param), 10, 64)
	if err != nil {
		return 0, err
	}
	return uint(id), nil
}

// parseFormIDs extracts a slice of uint IDs from a repeated form field.
// Invalid values are silently skipped.
func parseFormIDs(form url.Values, key string) []uint {
	var ids []uint
	for _, s := range form[key] {
		n, err := strconv.ParseUint(s, 10, 64)
		if err == nil {
			ids = append(ids, uint(n))
		}
	}
	return ids
}

// parseFormFloat parses a float64 from a form or query parameter.
// Returns 0 if the value is missing or not a valid number.
func parseFormFloat(r *http.Request, field string) float64 {
	v, _ := strconv.ParseFloat(r.FormValue(field), 64)
	return v
}

// parseFormDate parses a date string in "2006-01-02" format from a form field.
// Returns the zero time if missing or invalid.
func parseFormDate(r *http.Request, field string) time.Time {
	t, _ := time.Parse("2006-01-02", r.FormValue(field))
	return t
}

// nullString wraps a string in sql.NullString, marking it valid only when non-empty.
func nullString(s string) sql.NullString {
	return sql.NullString{String: s, Valid: s != ""}
}
