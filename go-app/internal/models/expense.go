package models

import (
	"time"

	"gorm.io/gorm"
)

type Expense struct {
	ID          uint           `gorm:"primaryKey" json:"id"`
	Name        string         `json:"name"`
	ExpenseDate time.Time      `gorm:"column:expense_date" json:"expense_date"`
	Cost        float64        `gorm:"column:cost" json:"cost"`
	CreatedAt   time.Time      `json:"created_at"`
	UpdatedAt   time.Time      `json:"updated_at"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"-"`
}
