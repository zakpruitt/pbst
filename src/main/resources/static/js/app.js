(function () {
  const cp    = document.getElementById('cp');
  const cpImg = cp.querySelector('img');

  document.addEventListener('mouseover', e => {
    const el = e.target.closest('[data-card-img]');
    if (el && el.dataset.cardImg) {
      cpImg.src = el.dataset.cardImg;
      cp.style.display = 'block';
    }
  });

  document.addEventListener('mouseout', e => {
    if (e.target.closest('[data-card-img]') && !e.relatedTarget?.closest('[data-card-img]')) {
      cp.style.display = 'none';
    }
  });

  document.addEventListener('mousemove', e => {
    if (cp.style.display !== 'none') {
      cp.style.left = (e.clientX + 18) + 'px';
      cp.style.top  = Math.max(8, e.clientY - 80) + 'px';
    }
  });
})();

document.addEventListener('submit', function (e) {
  var form = e.target;

  var confirmMessage = form.dataset.confirm;
  if (confirmMessage && !confirm(confirmMessage)) {
    e.preventDefault();
    return;
  }

  var requiredCheckbox = form.dataset.requireCheckbox;
  if (requiredCheckbox && !form.querySelector('input[name="' + requiredCheckbox + '"]:checked')) {
    e.preventDefault();
    alert('Select at least one item.');
    return;
  }

  if (!form.checkValidity()) {
    e.preventDefault();
    e.stopPropagation();
  }
  form.classList.add('was-validated');
}, true);

document.addEventListener('click', function (e) {
  var row = e.target.closest('[data-href]');
  if (row && !e.target.closest('a, button, input')) {
    window.location.href = row.dataset.href;
  }
});

function copyExpenseName(el) {
  var text = el.dataset.full || el.textContent;
  navigator.clipboard.writeText(text).then(function () {
    var original = el.textContent;
    el.textContent = 'Copied!';
    el.classList.add('text-success');
    setTimeout(function () { el.textContent = original; el.classList.remove('text-success'); }, 900);
  });
}
