package models

import (
	"database/sql"
	"time"

	"gorm.io/gorm"
)

type TrackedItem struct {
	ID                    uint               `gorm:"primaryKey" json:"id"`
	AcquisitionDate       time.Time          `gorm:"column:acquisition_date" json:"acquisition_date"`
	CostBasis             float64            `gorm:"column:cost_basis" json:"cost_basis"`
	MarketValueAtPurchase float64            `gorm:"column:market_value_at_purchase" json:"market_value_at_purchase"`
	ManualNameOverride    sql.NullString     `gorm:"column:manual_name_override" json:"manual_name_override"`
	Notes                 sql.NullString     `gorm:"type:text" json:"notes"`
	Purpose               string             `json:"purpose"`
	ItemType              string             `gorm:"column:item_type" json:"item_type"`
	LotPurchaseID         sql.NullInt64      `gorm:"column:lot_purchase_id" json:"lot_purchase_id"`
	LotPurchase           *LotPurchase       `json:"lot_purchase"`
	PokemonCardID         sql.NullString     `gorm:"column:pokemon_card_id" json:"pokemon_card_id"`
	PokemonCard           *PokemonCard       `json:"pokemon_card"`
	SealedProductID       sql.NullString     `gorm:"column:sealed_product_id" json:"sealed_product_id"`
	SealedProduct         *SealedProduct     `json:"sealed_product"`
	GradingSubmissionID   sql.NullInt64      `gorm:"column:grading_submission_id" json:"grading_submission_id"`
	GradingSubmission     *GradingSubmission `gorm:"foreignKey:GradingSubmissionID" json:"grading_submission,omitempty"`
	SaleID                sql.NullInt64      `gorm:"column:sale_id" json:"sale_id"`
	GradedDetails         *GradedDetails     `gorm:"embedded" json:"graded_details"`
	CreatedAt             time.Time          `json:"created_at"`
	UpdatedAt             time.Time          `json:"updated_at"`
	DeletedAt             gorm.DeletedAt     `gorm:"index" json:"-"`
}

type GradedDetails struct {
	GradingCompany  string  `gorm:"column:grading_company" json:"grading_company"`
	Grade           string  `gorm:"column:grade" json:"grade"`
	GradingUpcharge float64 `gorm:"column:grading_upcharge" json:"grading_upcharge"`
}
