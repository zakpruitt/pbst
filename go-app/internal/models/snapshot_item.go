package models

type SnapshotItem struct {
	Name           string  `json:"name"`
	PokemonCardID  string  `json:"pokemon_card_id,omitempty"`
	SetName        string  `json:"set_name,omitempty"`
	CardNumber     string  `json:"card_number,omitempty"`
	Rarity         string  `json:"rarity,omitempty"`
	Qty            int     `json:"qty"`
	MarketPrice    float64 `json:"market_price"`
	Percentage     float64 `json:"percentage"`
	Offered        float64 `json:"offered"`
	ItemType       string  `json:"item_type"` // RAW_CARD | GRADED_CARD | SEALED_PRODUCT
	IsTracked      bool    `json:"is_tracked"`
	Purpose        string  `json:"purpose,omitempty"` // PERSONAL_COLLECTION | PENDING_GRADE
	GradingCompany string  `json:"grading_company,omitempty"`
	Grade          string  `json:"grade,omitempty"`
	ImageURL       string  `json:"image_url,omitempty"`
}
