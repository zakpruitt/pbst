function createAmountInput(currentValue) {
    const input = document.createElement('input');
    input.type = 'number';
    input.step = '0.01';
    input.value = parseFloat(currentValue).toFixed(2);
    return input;
}

function saveAmounts(saleId, cell) {
    const grossEl = cell.querySelector('[data-field="gross"]');
    const netEl = cell.querySelector('[data-field="net"]');

    fetch('/sales/' + saleId + '/amounts?grossAmount=' + grossEl.dataset.value + '&netAmount=' + netEl.dataset.value, {
        method: 'PATCH',
    });
}

function formatAmount(label, value) {
    return label + ': $' + parseFloat(value).toFixed(2);
}

document.querySelectorAll('.editable-amount').forEach((el) => {
    el.addEventListener('dblclick', () => {
        if (el.querySelector('input')) return;

        const label = el.dataset.field === 'gross' ? 'Sold' : 'Net';
        const input = createAmountInput(el.dataset.value);

        el.textContent = label + ': $';
        el.appendChild(input);
        input.focus();
        input.select();

        function commit() {
            const newValue = parseFloat(input.value);
            if (isNaN(newValue)) {
                revert();
                return;
            }

            el.dataset.value = newValue.toFixed(2);
            el.textContent = formatAmount(label, newValue);
            saveAmounts(el.dataset.saleId, el.closest('td'));
        }

        function revert() {
            el.textContent = formatAmount(label, el.dataset.value);
        }

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') { e.preventDefault(); commit(); }
            if (e.key === 'Escape') { e.preventDefault(); revert(); }
        });
        input.addEventListener('blur', commit);
    });
});
