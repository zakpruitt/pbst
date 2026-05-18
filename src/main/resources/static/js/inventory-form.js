function inventoryItemSearch() {
    return {
        query: '',
        results: [],

        async search() {
            const q = this.query.trim();
            if (!q) { this.results = []; return; }

            const queryString = '?q=' + encodeURIComponent(q);
            const [cards, sealed] = await Promise.all([
                fetch('/api/v1/cards/search' + queryString).then((r) => r.json()).catch(() => []),
                fetch('/api/v1/sealed/search' + queryString).then((r) => r.json()).catch(() => []),
            ]);

            const cardRows = (cards || []).map((c) => ({ ...c, _kind: 'card' }));
            const sealedRows = (sealed || []).map((s) => ({ ...s, _kind: 'sealed' }));
            this.results = [...cardRows, ...sealedRows];
        },

        clear() {
            this.query = '';
            this.results = [];
        },

        async pick(card) {
            const isSealed = card._kind === 'sealed';
            this.clear();

            const params = new URLSearchParams({
                type: isSealed ? 'SEALED_PRODUCT' : 'RAW_CARD',
                name: card.name || '',
                set: card.setName || '',
                card: card.cardNumber || '',
                market: card.marketPrice || 0,
                img: card.imageUrl || '',
            });

            if (isSealed) {
                params.set('sealed_id', card.id || '');
            } else {
                params.set('card_id', card.id || '');
            }

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
            const html = await fetch('/inventory/partials/row?' + params).then((r) => r.text());
            document.getElementById('items-list').insertAdjacentHTML('beforeend', html);
            document.dispatchEvent(new CustomEvent('inv:changed'));
        },

        onEnter() {
            if (this.results.length === 0 && this.query.trim()) {
                this.addOther(this.query);
            }
        },
    };
}

function inventoryTotals() {
    return {
        count: 0,
        totalCost: 0,
        totalMarket: 0,

        recalc() {
            let count = 0;
            let cost = 0;
            let market = 0;

            collectItemRows().forEach((d) => {
                if (!d) return;
                count++;
                cost += d.costBasis || 0;
                market += d.market || 0;
            });

            this.count = count;
            this.totalCost = cost;
            this.totalMarket = market;
            updateEmptyMessage();
        },
    };
}

function serializeInventorySnapshot() {
    return collectItemRows().map((d) => ({
        name: d.name,
        item_type: d.type,
        cost_basis: d.costBasis || 0,
        market_value: d.market || 0,
        pokemon_card_id: d.cardId || '',
        sealed_product_id: d.sealedId || '',
        grading_company: d.gradingCompany || '',
        grade: d.grade || '',
    }));
}

document.getElementById('inventory-form').addEventListener('submit', () => {
    document.getElementById('items-snapshot-input').value = JSON.stringify(serializeInventorySnapshot());
});

document.addEventListener('alpine:init', () => {
    setTimeout(() => document.dispatchEvent(new CustomEvent('inv:changed')), 0);
});
