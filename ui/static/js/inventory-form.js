function inventoryItemSearch() {
    return {
        query: '',
        results: [],
        mode: 'cards',
        setMode(m) {
            this.mode = m;
            this.clear();
        },
        async search() {
            const q = this.query.trim();
            if (!q) { this.results = []; return; }
            const url = this.mode === 'sealed'
                ? '/api/v1/sealed/search'
                : '/api/v1/cards/search';
            const res = await fetch(url + '?q=' + encodeURIComponent(q));
            this.results = await res.json() || [];
        },
        clear() { this.query = ''; this.results = []; },
        async pick(c) {
            const sealed = this.mode === 'sealed';
            this.clear();
            const params = new URLSearchParams({
                type: sealed ? 'SEALED_PRODUCT' : 'RAW_CARD',
                name: c.name || '',
                set: c.set_name || '',
                card: c.card_number || '',
                market: c.market_price || 0,
                img: c.image_url || '',
            });
            if (sealed) {
                params.set('sealed_id', c.id || '');
            } else {
                params.set('card_id', c.id || '');
            }
            const html = await fetch('/inventory/partials/row?' + params).then(r => r.text());
            document.getElementById('items-list').insertAdjacentHTML('beforeend', html);
            document.dispatchEvent(new CustomEvent('inv:changed'));
        },
    };
}

function inventoryTotals() {
    return {
        count: 0, totalCost: 0, totalMarket: 0,
        recalc() {
            let n = 0, c = 0, m = 0;
            document.querySelectorAll('.item-row').forEach(row => {
                const d = Alpine.$data(row); if (!d) return;
                n++;
                c += d.costBasis || 0;
                m += d.market || 0;
            });
            this.count = n;
            this.totalCost = c;
            this.totalMarket = m;
            const list = document.getElementById('items-list');
            document.getElementById('empty-msg').style.display =
                list.children.length ? 'none' : '';
        },
    };
}

document.getElementById('inventory-form').addEventListener('submit', () => {
    const rows = [...document.querySelectorAll('.item-row')].map(row => {
        const d = Alpine.$data(row);
        return {
            name: d.name,
            item_type: d.type,
            cost_basis: d.costBasis || 0,
            market_value: d.market || 0,
            pokemon_card_id: d.cardId || '',
            sealed_product_id: d.sealedId || '',
            grading_company: d.gradingCompany || '',
            grade: d.grade || '',
        };
    });
    document.getElementById('items-snapshot-input').value = JSON.stringify(rows);
});

document.addEventListener('alpine:init', () => {
    setTimeout(() => document.dispatchEvent(new CustomEvent('inv:changed')), 0);
});
