function collectItemRows() {
    return [...document.querySelectorAll('.item-row')].map((row) => Alpine.$data(row));
}

function updateEmptyMessage() {
    const list = document.getElementById('items-list');
    document.getElementById('empty-msg').style.display = list.children.length ? 'none' : '';
}
