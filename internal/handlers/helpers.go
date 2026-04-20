package handlers

import (
	"database/sql"
	"net/http"
	"net/url"
	"strconv"
	"time"

	"github.com/zakpruitt/pbst/internal/models"
)

type MonthGroup[T any] struct {
	Label    string
	FirstDay time.Time
	Items    []T
}

// groupByMonth buckets a pre-sorted slice into contiguous month groups. The
// `at` function returns the time used for each item's bucket.
func groupByMonth[T any](items []T, at func(T) time.Time) []MonthGroup[T] {
	var groups []MonthGroup[T]
	var current string
	for _, item := range items {
		t := at(item)
		label := t.Format("January 2006")
		if label != current {
			groups = append(groups, MonthGroup[T]{Label: label, FirstDay: t})
			current = label
		}
		i := len(groups) - 1
		groups[i].Items = append(groups[i].Items, item)
	}
	return groups
}

// WithPathID wraps a handler that needs an `{id}` path parameter. It short-
// circuits with 404 when the id is missing or non-numeric.
func WithPathID(fn func(http.ResponseWriter, *http.Request, uint)) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		id, ok := requirePathID(w, r)
		if !ok {
			return
		}
		fn(w, r, id)
	}
}

func parsePathID(r *http.Request, param string) (uint, error) {
	id, err := strconv.ParseUint(r.PathValue(param), 10, 64)
	if err != nil {
		return 0, err
	}
	return uint(id), nil
}

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

func parseFormFloat(r *http.Request, field string) float64 {
	v, _ := strconv.ParseFloat(r.FormValue(field), 64)
	return v
}

func parseFormDate(r *http.Request, field string) time.Time {
	t, _ := time.Parse("2006-01-02", r.FormValue(field))
	return t
}

func queryParam(r *http.Request, key, fallback string) string {
	if v := r.URL.Query().Get(key); v != "" {
		return v
	}
	return fallback
}

func nullString(s string) sql.NullString {
	return sql.NullString{String: s, Valid: s != ""}
}

// groupInventoryByType splits inventory into the four buckets the inventory
// index template renders as separate tables.
func groupInventoryByType(items []models.TrackedItem) (raw, graded, sealed, other []models.TrackedItem) {
	for _, item := range items {
		switch item.ItemType {
		case "SEALED_PRODUCT":
			sealed = append(sealed, item)
		case "GRADED_CARD":
			graded = append(graded, item)
		case "OTHER":
			other = append(other, item)
		default:
			raw = append(raw, item)
		}
	}
	return
}

// splitInventoryItems returns (raw, graded) for pages that only offer these
// two choices — notably the sale-confirm and grading-edit selectors.
func splitInventoryItems(items []models.TrackedItem) (raw, graded []models.TrackedItem) {
	for _, item := range items {
		switch item.ItemType {
		case "GRADED_CARD":
			graded = append(graded, item)
		case "RAW_CARD":
			raw = append(raw, item)
		}
	}
	return
}
