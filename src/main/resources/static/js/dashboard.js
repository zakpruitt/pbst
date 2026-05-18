function getThemeColors() {
    const isDark = document.documentElement.getAttribute('data-bs-theme') === 'dark';

    return {
        accent: isDark ? '#2ecc71' : '#27ae60',
        muted: isDark ? '#9ca3af' : '#536471',
        danger: '#dc3545',
        blue: '#0d6efd',
        amber: '#ffc107',
        purple: '#6f42c1',
        teal: '#20c997',
        border: isDark ? '#2c3034' : '#e1e8ed',
        surface: isDark ? '#1a1d21' : '#ffffff',
        palette: [
            '#0d6efd', isDark ? '#2ecc71' : '#27ae60', '#ffc107',
            '#dc3545', '#6f42c1', '#20c997', '#fd7e14', '#0dcaf0',
        ],
    };
}

function formatMoney(value) {
    return '$' + Number(value).toLocaleString(undefined, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
    });
}

function createLineChart(canvasId, data, colors) {
    new Chart(document.getElementById(canvasId), {
        type: 'line',
        data: {
            labels: data.monthLabels,
            datasets: [
                {
                    label: 'Spent',
                    data: data.monthlySpend,
                    borderColor: colors.danger,
                    backgroundColor: 'rgba(220,53,69,0.08)',
                    tension: 0.3,
                    fill: true,
                    pointRadius: 3,
                },
                {
                    label: 'Gross',
                    data: data.monthlyGross,
                    borderColor: colors.blue,
                    backgroundColor: 'rgba(13,110,253,0.08)',
                    tension: 0.3,
                    fill: false,
                    pointRadius: 3,
                },
                {
                    label: 'Net',
                    data: data.monthlyNet,
                    borderColor: colors.accent,
                    backgroundColor: 'rgba(39,174,96,0.12)',
                    tension: 0.3,
                    fill: true,
                    pointRadius: 3,
                },
            ],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { padding: 16, boxWidth: 12 },
                },
                tooltip: {
                    callbacks: {
                        label: (ctx) => ' ' + ctx.dataset.label + ': ' + formatMoney(ctx.parsed.y),
                    },
                },
            },
            scales: {
                x: { grid: { color: colors.border } },
                y: { grid: { color: colors.border }, ticks: { callback: formatMoney } },
            },
        },
    });
}

function createDoughnut(canvasId, labels, values, colors, surface) {
    if (!labels.length) {
        const canvas = document.getElementById(canvasId);
        if (canvas) {
            canvas.parentElement.innerHTML = '<span class="text-muted small">No data yet.</span>';
        }
        return;
    }

    const total = values.reduce((a, b) => a + b, 0);

    new Chart(document.getElementById(canvasId), {
        type: 'doughnut',
        data: {
            labels: labels,
            datasets: [{
                data: values,
                backgroundColor: colors,
                borderColor: surface,
                borderWidth: 2,
                hoverOffset: 6,
            }],
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '60%',
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { padding: 12, boxWidth: 12, font: { size: 11 } },
                },
                tooltip: {
                    callbacks: {
                        label: (ctx) => {
                            const pct = total > 0 ? ((ctx.parsed / total) * 100).toFixed(1) : 0;
                            return ' ' + ctx.parsed + ' (' + pct + '%)';
                        },
                    },
                },
            },
        },
    });
}

function mapLabels(items, key, labelMap) {
    return items.map((r) => labelMap[r[key]] || r[key] || 'Unknown');
}

function mapCounts(items) {
    return items.map((r) => r.count);
}

function initDashboard(data) {
    const colors = getThemeColors();

    Chart.defaults.font.family = '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    Chart.defaults.font.size = 12;
    Chart.defaults.color = colors.muted;

    createLineChart('monthlyChart', data, colors);

    const itemTypeLabels = { RAW_CARD: 'Raw', GRADED_CARD: 'Graded', SEALED_PRODUCT: 'Sealed', OTHER: 'Other' };
    createDoughnut(
        'itemTypeChart',
        mapLabels(data.itemTypeCounts, 'itemType', itemTypeLabels),
        mapCounts(data.itemTypeCounts),
        colors.palette,
        colors.surface,
    );

    const originLabels = { EBAY: 'eBay', FACEBOOK: 'Facebook', OTHER: 'Other' };
    createDoughnut(
        'originChart',
        mapLabels(data.originCounts, 'origin', originLabels),
        mapCounts(data.originCounts),
        colors.palette,
        colors.surface,
    );

    createDoughnut(
        'gradingChart',
        data.gradingStatuses.map((r) => r.status),
        mapCounts(data.gradingStatuses),
        colors.palette,
        colors.surface,
    );
}
