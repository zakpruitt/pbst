package repository

import (
	"context"
	"fmt"

	"github.com/zakpruitt/pbst/internal/models"
	"gorm.io/gorm"
)

type ExpenseRepository interface {
	Create(ctx context.Context, e *models.Expense) error
	GetAll(ctx context.Context) ([]models.Expense, error)
	Delete(ctx context.Context, id uint) error
}

type expenseRepository struct {
	db *gorm.DB
}

func NewExpenseRepository(db *gorm.DB) ExpenseRepository {
	return &expenseRepository{db: db}
}

func (r *expenseRepository) Create(ctx context.Context, e *models.Expense) error {
	err := r.db.WithContext(ctx).Create(e).Error
	if err != nil {
		return fmt.Errorf("create expense: %w", err)
	}
	return nil
}

// GetAll returns all expenses newest first so the template can group by month
// contiguously without re-sorting.
func (r *expenseRepository) GetAll(ctx context.Context) ([]models.Expense, error) {
	var expenses []models.Expense
	err := r.db.WithContext(ctx).Order("expense_date DESC, id DESC").Find(&expenses).Error
	if err != nil {
		return nil, fmt.Errorf("get all expenses: %w", err)
	}
	return expenses, nil
}

func (r *expenseRepository) Delete(ctx context.Context, id uint) error {
	err := r.db.WithContext(ctx).Delete(&models.Expense{}, id).Error
	if err != nil {
		return fmt.Errorf("delete expense: %w", err)
	}
	return nil
}
