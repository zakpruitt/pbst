function collectItemRows() {
    return [...document.querySelectorAll('.item-row')].map(function (row) { return Alpine.$data(row); });
}

function updateEmptyMessage() {
    var list = document.getElementById('items-list');
    document.getElementById('empty-msg').style.display = list.children.length ? 'none' : '';
}

function createItemSearch(config) {
    return {
        query: '',
        results: [],

        async search() {
            var q = this.query.trim();
            if (!q) { this.results = []; return; }

            var queryString = '?q=' + encodeURIComponent(q);
            var fetches = config.endpoints.map(function (ep) {
                return fetch(ep.url + queryString).then(function (r) { return r.json(); }).catch(function () { return []; });
            });

            var responses = await Promise.all(fetches);
            var merged = [];
            responses.forEach(function (items, i) {
                var mapFn = config.endpoints[i].map;
                (items || []).forEach(function (item) {
                    merged.push(mapFn ? mapFn(item) : item);
                });
            });
            this.results = merged;
        },

        clear() {
            this.query = '';
            this.results = [];
        },

        async pick(card) {
            var params = config.buildParams(card);
            this.clear();
            await this.appendRow(params);
        },

        async addOther(name) {
            var params = new URLSearchParams({
                itemType: 'OTHER',
                name: (name || this.query || '').trim(),
            });
            this.clear();
            await this.appendRow(params);
        },

        async appendRow(params) {
            var html = await fetch(config.partialUrl + '?' + params).then(function (r) { return r.text(); });
            var list = document.getElementById('items-list');
            list.insertAdjacentHTML('beforeend', html);
            Alpine.initTree(list.lastElementChild);
            setTimeout(function () { window.dispatchEvent(new CustomEvent(config.changeEvent)); }, 0);
        },

        onEnter() {
            if (this.results.length === 0 && this.query.trim()) {
                this.addOther(this.query);
            }
        },
    };
}

function cardSearch() {
    return createItemSearch({
        endpoints: [{ url: '/api/v1/cards/search' }],
        partialUrl: '/lots/partials/row',
        changeEvent: 'lot:changed',
        buildParams: function (card) {
            return new URLSearchParams({
                pokemonCardId: card.id || '',
                name: card.name || '',
                setName: card.setName || '',
                cardNumber: card.cardNumber || '',
                rarity: card.rarity || '',
                marketPrice: card.marketPrice || 0,
                imageUrl: card.imageUrl || '',
            });
        },
    });
}

function inventoryItemSearch() {
    return createItemSearch({
        endpoints: [
            { url: '/api/v1/cards/search', map: function (c) { return Object.assign({}, c, { _kind: 'card' }); } },
            { url: '/api/v1/sealed/search', map: function (s) { return Object.assign({}, s, { _kind: 'sealed' }); } },
        ],
        partialUrl: '/inventory/partials/row',
        changeEvent: 'inv:changed',
        buildParams: function (card) {
            var isSealed = card._kind === 'sealed';
            var params = new URLSearchParams({
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
            return params;
        },
    });
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
            var totalEmv = 0;
            var totalBuying = 0;
            var trackedEmv = 0;
            var untrackedBuying = 0;

            collectItemRows().forEach(function (d) {
                if (!d) return;
                var emv = (d.market || 0) * (d.qty || 1);
                var buying = emv * ((d.pct || 0) / 100);
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

function inventoryTotals() {
    return {
        count: 0,
        totalCost: 0,
        totalMarket: 0,

        recalc() {
            var count = 0;
            var cost = 0;
            var market = 0;

            collectItemRows().forEach(function (d) {
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

function serializeLotSnapshot() {
    var totalCost = 0;
    var totalEmv = 0;

    var items = collectItemRows().map(function (d) {
        var offered = d.qty * d.market * d.pct / 100;
        totalCost += offered;
        totalEmv += d.qty * d.market;

        return {
            name: d.name,
            pokemon_card_id: d.cardId || null,
            set_name: d.setName || null,
            card_number: d.cardNumber || null,
            rarity: d.rarity || null,
            image_url: d.imageUrl || null,
            qty: d.qty,
            market_price: d.market,
            percentage: d.pct,
            offered: offered,
            item_type: d.type,
            is_tracked: d.tracked,
            purpose: d.tracked ? 'INVENTORY' : null,
            grading_company: d.gradingCompany || null,
            grade: d.grade || null,
        };
    });

    return { items: items, totalCost: totalCost, totalEmv: totalEmv };
}

function serializeInventorySnapshot() {
    return collectItemRows().map(function (d) {
        return {
            name: d.name,
            itemType: d.type,
            costBasis: d.costBasis || 0,
            marketValue: d.market || 0,
            pokemonCardId: d.cardId || null,
            sealedProductId: d.sealedId || null,
            gradingCompany: d.gradingCompany || null,
            grade: d.grade || null,
        };
    });
}

function initFormSubmit(formId, buildBody, changeEvent) {
    document.getElementById(formId).addEventListener('submit', async function (e) {
        e.preventDefault();
        var form = e.target;
        form.classList.add('was-validated');
        if (!form.checkValidity()) return;

        var body = buildBody(new FormData(form));
        var response = await fetch(form.action, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body),
        });

        if (response.ok) {
            window.location.href = await response.text();
        }
    });

    document.addEventListener('alpine:init', function () {
        setTimeout(function () { window.dispatchEvent(new CustomEvent(changeEvent)); }, 0);
    });
}

document.addEventListener('alpine:init', function () {
    Alpine.data('lotRow', function () {
        return {
            name: '', qty: 1, market: 0, pct: 0, type: 'RAW_CARD', tracked: false,
            gradingCompany: 'PSA', grade: '', cardId: '', imageUrl: '',
            setName: '', cardNumber: '', rarity: '',

            init() {
                var ds = this.$el.dataset;
                this.name = ds.name || '';
                this.qty = parseInt(ds.qty) || 1;
                this.market = parseFloat(ds.market) || 0;
                this.pct = parseFloat(ds.pct) || 0;
                this.type = ds.type || 'RAW_CARD';
                this.tracked = ds.tracked === 'true';
                this.gradingCompany = ds.gradingCompany || 'PSA';
                this.grade = ds.grade || '';
                this.cardId = ds.cardId || '';
                this.imageUrl = ds.imageUrl || '';
                this.setName = ds.setName || '';
                this.cardNumber = ds.cardNumber || '';
                this.rarity = ds.rarity || '';
            },

            get offered() { return (this.qty * this.market * this.pct / 100).toFixed(2); },
            get isGraded() { return this.type === 'GRADED_CARD'; },
            get borderColor() {
                if (this.type === 'GRADED_CARD') return '#f0b429';
                if (this.type === 'SEALED_PRODUCT') return '#0d6efd';
                return 'transparent';
            },
        };
    });

    Alpine.data('inventoryRow', function () {
        return {
            name: '', costBasis: 0, market: 0, type: 'RAW_CARD',
            gradingCompany: 'PSA', grade: '', cardId: '', sealedId: '',
            imageUrl: '', setName: '', cardNumber: '',

            init() {
                var ds = this.$el.dataset;
                this.name = ds.name || '';
                this.costBasis = parseFloat(ds.costBasis) || 0;
                this.market = parseFloat(ds.market) || 0;
                this.type = ds.type || 'RAW_CARD';
                this.gradingCompany = ds.gradingCompany || 'PSA';
                this.grade = ds.grade || '';
                this.cardId = ds.cardId || '';
                this.sealedId = ds.sealedId || '';
                this.imageUrl = ds.imageUrl || '';
                this.setName = ds.setName || '';
                this.cardNumber = ds.cardNumber || '';
            },

            get isGraded() { return this.type === 'GRADED_CARD'; },
            get borderColor() {
                if (this.type === 'GRADED_CARD') return '#f0b429';
                if (this.type === 'SEALED_PRODUCT') return '#0d6efd';
                if (this.type === 'OTHER') return '#888';
                return 'transparent';
            },
        };
    });
});

if (document.getElementById('lot-form')) {
    initFormSubmit('lot-form', function (formData) {
        var snapshot = serializeLotSnapshot();
        return {
            sellerName: formData.get('sellerName'),
            purchaseDate: formData.get('purchaseDate'),
            description: formData.get('description') || null,
            totalCost: snapshot.totalCost,
            estimatedMarketValue: snapshot.totalEmv,
            items: snapshot.items,
        };
    }, 'lot:changed');
}

if (document.getElementById('inventory-form')) {
    initFormSubmit('inventory-form', function (formData) {
        return {
            items: serializeInventorySnapshot(),
            purpose: formData.get('purpose'),
            acquisitionDate: formData.get('acquisitionDate'),
        };
    }, 'inv:changed');
}
