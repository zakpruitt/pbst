document.addEventListener('input', function (e) {
    if (e.target.classList.contains('grade-input')) {
        e.target.classList.remove('is-invalid');
    }
});

function collectGrades() {
    const grades = [];

    document.querySelectorAll('tr[data-item-id]').forEach((row) => {
        grades.push({
            itemId: parseInt(row.dataset.itemId),
            grade: row.querySelector('.grade-input').value.trim(),
            upcharge: parseFloat(row.querySelector('.upcharge-input').value) || 0,
        });
    });

    return grades;
}

function recordReturn(submissionId) {
    const inputs = document.querySelectorAll('.grade-input');
    let valid = true;
    inputs.forEach(function (input) {
        if (!input.value.trim()) {
            input.classList.add('is-invalid');
            valid = false;
        } else {
            input.classList.remove('is-invalid');
        }
    });
    if (!valid) return;

    const grades = collectGrades();

    fetch('/grading/' + submissionId + '/status?action=RETURN', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(grades),
    }).then((response) => {
        if (response.ok || response.redirected) {
            window.location.reload();
        } else {
            alert('Error recording return');
        }
    });
}
