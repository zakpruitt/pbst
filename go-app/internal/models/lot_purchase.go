package models

import (
	"encoding/json"
	"time"

	"gorm.io/gorm"
)

type LotPurchase struct {
	ID                   uint           `gorm:"primaryKey" json:"id"`
	SellerName           string         `gorm:"column:seller_name" json:"seller_name"`
	PurchaseDate         time.Time      `gorm:"column:purchase_date" json:"purchase_date"`
	Description          string         `gorm:"type:text" json:"description"`
	TotalCost            float64        `gorm:"column:total_cost" json:"total_cost"`
	EstimatedMarketValue float64        `gorm:"column:estimated_market_value" json:"estimated_market_value"`
	LotContentSnapshot   string         `gorm:"type:text" json:"lot_content_snapshot"`
	Status               string         `json:"status"`
	TrackedItems         []TrackedItem  `gorm:"foreignKey:LotPurchaseID" json:"tracked_items"`
	CreatedAt            time.Time      `json:"created_at"`
	UpdatedAt            time.Time      `json:"updated_at"`
	DeletedAt            gorm.DeletedAt `gorm:"index" json:"-"`
}

func (l *LotPurchase) ParseSnapshot() ([]SnapshotItem, error) {
	if l.LotContentSnapshot == "" {
		return nil, nil
	}
	var items []SnapshotItem
	if err := json.Unmarshal([]byte(l.LotContentSnapshot), &items); err != nil {
		return nil, err
	}
	return items, nil
}
