package models

import (
	"database/sql"
	"time"

	"gorm.io/gorm"
)

type Sale struct {
	ID            uint           `gorm:"primaryKey" json:"id"`
	EbayOrderID   string         `gorm:"column:ebay_order_id;unique" json:"ebay_order_id"`
	SaleDate      time.Time      `gorm:"column:sale_date" json:"sale_date"`
	Title         string         `json:"title"`
	BuyerUsername string         `gorm:"column:buyer_username" json:"buyer_username"`
	GrossAmount   float64        `gorm:"column:gross_amount" json:"gross_amount"`
	EbayFees      float64        `gorm:"column:ebay_fees" json:"ebay_fees"`
	ShippingCost  float64        `gorm:"column:shipping_cost" json:"shipping_cost"`
	NetAmount     float64        `gorm:"column:net_amount" json:"net_amount"`
	ImageURL      string         `gorm:"column:image_url" json:"image_url"`
	OrderStatus   string         `gorm:"column:order_status" json:"order_status"`
	Origin        string         `gorm:"column:origin;default:EBAY" json:"origin"`
	Status        string         `gorm:"column:status;default:STAGED" json:"status"`
	AttributedTo  string         `gorm:"column:attributed_to" json:"attributed_to"`
	Notes         sql.NullString `gorm:"type:text" json:"notes"`
	Items         []TrackedItem  `gorm:"foreignKey:SaleID" json:"items,omitempty"`
	CreatedAt     time.Time      `json:"created_at"`
	UpdatedAt     time.Time      `json:"updated_at"`
	DeletedAt     gorm.DeletedAt `gorm:"index" json:"-"`
}
