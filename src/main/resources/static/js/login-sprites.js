const SPRITE_BASE_REGULAR = 'https://raw.githubusercontent.com/msikma/pokesprite/refs/heads/master/pokemon-gen7x/regular/';
const SPRITE_BASE_SHINY = 'https://raw.githubusercontent.com/msikma/pokesprite/refs/heads/master/pokemon-gen7x/shiny/';

function createSprite() {
    const name = POKEMON_NAMES[Math.floor(Math.random() * POKEMON_NAMES.length)];
    const base = Math.random() < 0.28 ? SPRITE_BASE_SHINY : SPRITE_BASE_REGULAR;
    const size = 80 + Math.floor(Math.random() * 80);
    const duration = 16 + Math.random() * 18;
    const delay = -(Math.random() * duration);

    const img = document.createElement('img');
    img.src = base + name + '.png';
    img.className = 'sprite';
    img.style.cssText = [
        'width:' + size + 'px',
        'left:' + (Math.random() * 100) + '%',
        'top:' + (8 + Math.random() * 84) + '%',
        'animation-duration:' + duration + 's',
        'animation-delay:' + delay + 's',
    ].join(';');
    img.onerror = function () { this.remove(); };

    return img;
}

const container = document.getElementById('sprites');
for (let i = 0; i < 12; i++) {
    container.appendChild(createSprite());
}
