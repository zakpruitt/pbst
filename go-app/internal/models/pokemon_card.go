package models

import (
	"database/sql"
	"time"

	"gorm.io/gorm"
)

type PokemonCard struct {
	ID            string         `gorm:"primaryKey" json:"id"`
	Name          string         `json:"name"`
	SetCode       string         `gorm:"column:set_code" json:"set_code"`
	SetName       string         `gorm:"column:set_name" json:"set_name"`
	CardNumber    string         `gorm:"column:card_number" json:"card_number"`
	Rarity        string         `json:"rarity"`
	ImageURL      string         `gorm:"column:image_url" json:"image_url"`
	MarketPrice   float64        `gorm:"column:market_price" json:"market_price"`
	LowPrice      float64        `gorm:"column:low_price" json:"low_price"`
	LastPriceSync sql.NullTime   `gorm:"column:last_price_sync" json:"last_price_sync"`
	CreatedAt     time.Time      `json:"created_at"`
	UpdatedAt     time.Time      `json:"updated_at"`
	DeletedAt     gorm.DeletedAt `gorm:"index" json:"-"`
}
