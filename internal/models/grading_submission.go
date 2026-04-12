package models

import (
	"database/sql"
	"time"

	"gorm.io/gorm"
)

type GradingSubmission struct {
	ID               uint           `gorm:"primaryKey" json:"id"`
	SubmissionName   string         `gorm:"column:submission_name" json:"submission_name"`
	SendDate         sql.NullTime   `gorm:"column:send_date" json:"send_date"`
	ReturnDate       sql.NullTime   `gorm:"column:return_date" json:"return_date"`
	Company          string         `json:"company"`
	Status           string         `json:"status"`
	SubmissionMethod string         `gorm:"column:submission_method" json:"submission_method"`
	Notes            sql.NullString `gorm:"type:text" json:"notes"`
	CostPerCard      float64        `gorm:"column:cost_per_card" json:"cost_per_card"`
	TaxRate          float64        `gorm:"column:tax_rate" json:"tax_rate"`
	SubmissionCost   float64        `gorm:"column:submission_cost" json:"submission_cost"`
	UpchargeTotal    float64        `gorm:"column:upcharge_total" json:"upcharge_total"`
	Items            []TrackedItem  `gorm:"foreignKey:GradingSubmissionID" json:"items"`
	CreatedAt        time.Time      `json:"created_at"`
	UpdatedAt        time.Time      `json:"updated_at"`
	DeletedAt        gorm.DeletedAt `gorm:"index" json:"-"`
}

func (s *GradingSubmission) GrandTotal() float64 {
	return s.SubmissionCost + s.UpchargeTotal
}
