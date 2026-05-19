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
