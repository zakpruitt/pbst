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
	TotalGradingCost float64        `gorm:"column:total_grading_cost" json:"total_grading_cost"`
	Items            []TrackedItem  `gorm:"foreignKey:GradingSubmissionID" json:"items"`
	CreatedAt        time.Time      `json:"created_at"`
	UpdatedAt        time.Time      `json:"updated_at"`
	DeletedAt        gorm.DeletedAt `gorm:"index" json:"-"`
}
