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
