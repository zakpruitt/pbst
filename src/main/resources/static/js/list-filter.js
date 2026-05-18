function listFilter(groupClass, rowClass, fields) {
    return {
        query: '',
        origin: '',
        status: '',
        summary: '',

        apply() {
            const q = this.query.trim().toLowerCase();
            const origin = this.origin;
            const status = this.status;
            let shown = 0;

            document.querySelectorAll('.' + groupClass).forEach((group) => {
                let groupShown = 0;

                group.querySelectorAll('.' + rowClass).forEach((row) => {
                    let textMatch = !q;
                    if (q) {
                        for (const field of fields) {
                            if ((row.dataset[field] || '').toLowerCase().includes(q)) {
                                textMatch = true;
                                break;
                            }
                        }
                    }

                    const originMatch = !origin || (row.dataset.origin || '') === origin;
                    const statusMatch = !status || (row.dataset.status || '') === status;
                    const visible = textMatch && originMatch && statusMatch;

                    row.style.display = visible ? '' : 'none';
                    if (visible) {
                        groupShown++;
                        shown++;
                    }
                });

                group.style.display = groupShown === 0 ? 'none' : '';
            });

            this.summary = (q || origin || status)
                ? shown + ' match' + (shown === 1 ? '' : 'es')
                : '';
        },
    };
}

function saleFilter() {
    return listFilter('sale-month-group', 'sale-row', ['title', 'buyer']);
}

function lotFilter() {
    return listFilter('lot-month-group', 'lot-row', ['seller']);
}

function gradingFilter() {
    return listFilter('grading-month-group', 'grading-row', ['name', 'company']);
}
