/* Tema se postavlja prije prikaza stranice kako ne bi bljesnula svijetla pozadina. */
(() => {
    const kljuc = 'zupni-ured-tema';
    let spremljenaTema;
    try {
        spremljenaTema = localStorage.getItem(kljuc);
    } catch {
        // Promjena teme radi i kada preglednik ne dopušta spremanje postavki.
    }
    const zadanaTema = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    document.documentElement.dataset.bsTheme =
        spremljenaTema === 'dark' || spremljenaTema === 'light' ? spremljenaTema : zadanaTema;

    document.addEventListener('DOMContentLoaded', () => {
        const gumb = document.getElementById('promjena-teme');
        if (!gumb) return;

        function osvjeziGumb() {
            const tamnaTema = document.documentElement.dataset.bsTheme === 'dark';
            gumb.textContent = tamnaTema ? 'Svijetli način' : 'Tamni način';
            gumb.setAttribute('aria-label', tamnaTema ? 'Uključi svijetli način' : 'Uključi tamni način');
            gumb.setAttribute('aria-pressed', String(tamnaTema));
        }

        gumb.hidden = false;
        osvjeziGumb();
        gumb.addEventListener('click', () => {
            const tema = document.documentElement.dataset.bsTheme === 'dark' ? 'light' : 'dark';
            document.documentElement.dataset.bsTheme = tema;
            try {
                localStorage.setItem(kljuc, tema);
            } catch {
                // Odabir ostaje aktivan na otvorenoj stranici.
            }
            osvjeziGumb();
        });
    });
})();
