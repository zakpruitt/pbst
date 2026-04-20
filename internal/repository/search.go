package repository

import (
	"context"

	"gorm.io/gorm"
)

// searchByName performs an ILIKE search on name and set_name columns, returning
// up to `limit` results. Used by pokemon card and sealed product repositories.
func searchByName[T any](ctx context.Context, db *gorm.DB, query string, limit int) ([]T, error) {
	var results []T
	like := "%" + query + "%"
	err := db.WithContext(ctx).
		Where("name ILIKE ? OR set_name ILIKE ?", like, like).
		Limit(limit).
		Find(&results).Error
	return results, err
}
