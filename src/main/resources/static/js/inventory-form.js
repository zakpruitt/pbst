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
                itemType: isSealed ? 'SEALED_PRODUCT' : 'RAW_CARD',
                name: card.name || '',
                setName: card.setName || '',
                cardNumber: card.cardNumber || '',
                marketValue: card.marketPrice || 0,
                imageUrl: card.imageUrl || '',
            });

            if (isSealed) {
                params.set('sealedProductId', card.id || '');
            } else {
                params.set('pokemonCardId', card.id || '');
            }

            await this.appendRow(params);
        },

        async addOther(name) {
            const params = new URLSearchParams({
                itemType: 'OTHER',
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
        itemType: d.type,
        costBasis: d.costBasis || 0,
        marketValue: d.market || 0,
        pokemonCardId: d.cardId || null,
        sealedProductId: d.sealedId || null,
        gradingCompany: d.gradingCompany || null,
        grade: d.grade || null,
    }));
}

document.getElementById('inventory-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const formData = new FormData(form);

    const body = {
        items: serializeInventorySnapshot(),
        purpose: formData.get('purpose'),
        acquisitionDate: formData.get('acquisitionDate'),
    };

    const response = await fetch(form.action, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });

    if (response.ok) {
        window.location.href = await response.text();
    }
});

document.addEventListener('alpine:init', () => {
    setTimeout(() => document.dispatchEvent(new CustomEvent('inv:changed')), 0);
});
