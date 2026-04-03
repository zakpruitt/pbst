package handlers

import (
	"net/http"
	"net/url"
	"strconv"
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
