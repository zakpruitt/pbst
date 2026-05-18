function cardSearch() {
    return {
        query: '',
        results: [],

        async search() {
            const q = this.query.trim();
            if (!q) { this.results = []; return; }

            const response = await fetch('/api/v1/cards/search?q=' + encodeURIComponent(q));
            this.results = await response.json() || [];
        },

        clear() {
            this.query = '';
            this.results = [];
        },

        async pick(card) {
            const params = new URLSearchParams({
                card_id: card.id || '',
                name: card.name || '',
                set: card.setName || '',
                card: card.cardNumber || '',
                rarity: card.rarity || '',
                market: card.marketPrice || 0,
                img: card.imageUrl || '',
            });
            this.clear();
            await this.appendRow(params);
        },

        async addOther(name) {
            const params = new URLSearchParams({
                type: 'OTHER',
                name: (name || this.query || '').trim(),
            });
            this.clear();
            await this.appendRow(params);
        },

        async appendRow(params) {
            const html = await fetch('/lots/partials/row?' + params).then((r) => r.text());
            document.getElementById('items-list').insertAdjacentHTML('beforeend', html);
            document.dispatchEvent(new CustomEvent('lot:changed'));
        },

        onEnter() {
            if (this.results.length === 0 && this.query.trim()) {
                this.addOther(this.query);
            }
        },
    };
}

function lotTotals() {
    return {
        totalEmv: 0,
        totalBuying: 0,
        trackedEmv: 0,
        untrackedEmv: 0,
        estFlipNet: 0,
        estFlipGross: 0,

        recalc() {
            let totalEmv = 0;
            let totalBuying = 0;
            let trackedEmv = 0;
            let untrackedBuying = 0;

            collectItemRows().forEach((d) => {
                if (!d) return;

                const emv = (d.market || 0) * (d.qty || 1);
                const buying = emv * ((d.pct || 0) / 100);

                totalEmv += emv;
                totalBuying += buying;

                if (d.tracked) {
                    trackedEmv += emv;
                } else {
                    untrackedBuying += buying;
                }
            });

            this.totalEmv = totalEmv;
            this.totalBuying = totalBuying;
            this.trackedEmv = trackedEmv;
            this.untrackedEmv = totalEmv - trackedEmv;
            this.estFlipNet = (this.untrackedEmv * 0.88) - untrackedBuying;
            this.estFlipGross = this.untrackedEmv - untrackedBuying;
            updateEmptyMessage();
        },
    };
}

function serializeLotSnapshot() {
    let totalCost = 0;
    let totalEmv = 0;

    const rows = collectItemRows().map((d) => {
        const offered = d.qty * d.market * d.pct / 100;
        totalCost += offered;
        totalEmv += d.qty * d.market;

        return {
            name: d.name,
            pokemon_card_id: d.cardId,
            set_name: d.setName,
            card_number: d.cardNumber,
            rarity: d.rarity,
            image_url: d.imageUrl,
            qty: d.qty,
            market_price: d.market,
            percentage: d.pct,
            offered,
            item_type: d.type,
            is_tracked: d.tracked,
            purpose: d.tracked ? 'INVENTORY' : '',
            grading_company: d.gradingCompany,
            grade: d.grade,
        };
    });

    return { rows, totalCost, totalEmv };
}

document.getElementById('lot-form').addEventListener('submit', () => {
    const snapshot = serializeLotSnapshot();
    document.getElementById('snapshot-input').value = JSON.stringify(snapshot.rows);
    document.getElementById('total-cost-input').value = snapshot.totalCost.toFixed(2);
    document.getElementById('emv-input').value = snapshot.totalEmv.toFixed(2);
});

document.addEventListener('alpine:init', () => {
    setTimeout(() => document.dispatchEvent(new CustomEvent('lot:changed')), 0);
});
