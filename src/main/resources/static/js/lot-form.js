function cardSearch() {
    return {
        query: '',
        results: [],
        async search() {
            const q = this.query.trim();
            if (!q) { this.results = []; return; }
            const res = await fetch('/api/v1/cards/search?q=' + encodeURIComponent(q));
            this.results = await res.json() || [];
        },
        clear() { this.query = ''; this.results = []; },
        async pick(c) {
            const params = new URLSearchParams({
                card_id: c.id || '', name: c.name || '', set: c.setName || '',
                card: c.cardNumber || '', rarity: c.rarity || '',
                market: c.marketPrice || 0, img: c.imageUrl || '',
            });
            this.clear();
            await this.appendRow(params);
        },
        async addOther(name) {
            const params = new URLSearchParams({ type: 'OTHER', name: (name || this.query || '').trim() });
            this.clear();
            await this.appendRow(params);
        },
        async appendRow(params) {
            const html = await fetch('/lots/partials/row?' + params).then(r => r.text());
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
        totalEmv: 0, totalBuying: 0, trackedEmv: 0, untrackedEmv: 0,
        estFlipNet: 0, estFlipGross: 0,
        recalc() {
            let te = 0, tb = 0, trk = 0, uBuy = 0;
            document.querySelectorAll('.item-row').forEach(row => {
                const d = Alpine.$data(row); if (!d) return;
                const emv = (d.market || 0) * (d.qty || 1);
                const buy = emv * ((d.pct || 0) / 100);
                te += emv; tb += buy;
                d.tracked ? trk += emv : uBuy += buy;
            });
            this.totalEmv = te; this.totalBuying = tb; this.trackedEmv = trk;
            this.untrackedEmv = te - trk;
            this.estFlipNet   = (this.untrackedEmv * 0.88) - uBuy;
            this.estFlipGross = this.untrackedEmv - uBuy;
            const list = document.getElementById('items-list');
            document.getElementById('empty-msg').style.display =
                list.children.length ? 'none' : '';
        },
    };
}

document.getElementById('lot-form').addEventListener('submit', () => {
    let totalCost = 0, totalEmv = 0;
    const rows = [...document.querySelectorAll('.item-row')].map(row => {
        const d = Alpine.$data(row);
        const offered = d.qty * d.market * d.pct / 100;
        totalCost += offered; totalEmv += d.qty * d.market;
        return {
            name: d.name, pokemon_card_id: d.cardId, set_name: d.setName,
            card_number: d.cardNumber, rarity: d.rarity, image_url: d.imageUrl,
            qty: d.qty, market_price: d.market, percentage: d.pct, offered,
            item_type: d.type, is_tracked: d.tracked,
            purpose: d.tracked ? 'INVENTORY' : '',
            grading_company: d.gradingCompany, grade: d.grade,
        };
    });
    document.getElementById('snapshot-input').value   = JSON.stringify(rows);
    document.getElementById('total-cost-input').value = totalCost.toFixed(2);
    document.getElementById('emv-input').value        = totalEmv.toFixed(2);
});

document.addEventListener('alpine:init', () => {
    setTimeout(() => document.dispatchEvent(new CustomEvent('lot:changed')), 0);
});
