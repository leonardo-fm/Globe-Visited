/**
 * Genera dev/icon-preview.html: l'icona di adesso accanto a qualche proposta
 * costruita coi confini veri, cosi' si sceglie guardando invece che a parole.
 *
 * node dev/make-icon-preview.mjs
 */
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';

const OCEAN = '#1B4B8F', LAND = '#8A8A8A', VISITED = '#FF8C1A', SPACE = '#05070F';

function globe(opts) {
  const args = [];
  for (const [k, v] of Object.entries(opts)) args.push('--' + k, String(v));
  const out = execFileSync('node', ['tools/build-icon.mjs', ...args], { encoding: 'utf8' });
  return JSON.parse(out);
}

// Tre livelli di dettaglio, per vedere quanto si puo' semplificare prima che
// il pianeta smetta di sembrare il pianeta.
const R = 33;   // zona sicura dell'icona adattiva: cerchio di diametro 66 su 108
const uno    = globe({ lon: 10, lat: 25, accent: 'ITA', tol: 0.6, minarea: 0.6, r: R });
const medit  = globe({ lon: 10, lat: 25, r: R, tol: 0.6, minarea: 0.6,
                       accent: 'ITA,FRA,ESP,PRT,GRC,MAR,EGY,TUR,HRV' });
const atl    = globe({ lon: -35, lat: 12, accent: 'BRA', tol: 0.6, minarea: 0.6, r: R });
const misto  = globe({ lon: -20, lat: 20, r: R, tol: 0.6, minarea: 0.6,
                       accent: 'BRA,ITA,ESP,FRA,MAR,USA,MEX' });

function sphere(g, landColor, accentColor, withShade) {
  const id = 'g' + Math.random().toString(36).slice(2, 8);
  return `
    <defs>
      <clipPath id="c${id}"><circle cx="${g.cx}" cy="${g.cy}" r="${g.r}"/></clipPath>
      ${withShade ? `<radialGradient id="s${id}" cx="36%" cy="30%" r="80%">
        <stop offset="0%" stop-color="#2f6dc0"/>
        <stop offset="65%" stop-color="${OCEAN}"/>
        <stop offset="100%" stop-color="#0f2a4a"/>
      </radialGradient>` : ''}
    </defs>
    <circle cx="${g.cx}" cy="${g.cy}" r="${g.r}" fill="${withShade ? `url(#s${id})` : OCEAN}"/>
    <g clip-path="url(#c${id})">
      <path d="${g.land}" fill="${landColor}"/>
      <path d="${g.accent}" fill="${accentColor}"/>
    </g>`;
}

const OPTIONS = [
  {
    name: 'Adesso',
    sub: 'sfera grigia, continenti inventati',
    bg: OCEAN,
    weight: '~0,4 KB',
    fg: `
      <circle cx="54" cy="54" r="24" fill="${LAND}"/>
      <path d="M30,54L78,54" stroke="${SPACE}" stroke-width="2" stroke-opacity=".55"/>
      <ellipse cx="54" cy="54" rx="13" ry="24" fill="none" stroke="${SPACE}" stroke-width="2" stroke-opacity=".55"/>
      <circle cx="70" cy="43" r="7" fill="${VISITED}"/>`
  },
  {
    name: 'A — un paese solo',
    sub: `Europa e Africa, Italia accesa · ${(uno.land.length / 1024).toFixed(1)} KB`,
    bg: SPACE,
    fg: sphere(uno, LAND, VISITED, true)
  },
  {
    name: 'B — Mediterraneo acceso',
    sub: `stessa vista, nove paesi · ${(medit.land.length / 1024).toFixed(1)} KB`,
    bg: SPACE,
    fg: sphere(medit, LAND, VISITED, true)
  },
  {
    name: 'C — Atlantico',
    sub: `Americhe e Africa, Brasile acceso · ${(atl.land.length / 1024).toFixed(1)} KB`,
    bg: SPACE,
    fg: sphere(atl, LAND, VISITED, true)
  },
  {
    name: 'D — Atlantico, più paesi',
    sub: `un viaggiatore vero · ${(misto.land.length / 1024).toFixed(1)} KB`,
    bg: SPACE,
    fg: sphere(misto, LAND, VISITED, true)
  }
];

const cell = (o, size, mask, safe) => `
  <div class="cell">
    <svg width="${size}" height="${size}" viewBox="0 0 108 108" class="${mask}">
      <rect width="108" height="108" fill="${o.bg}"/>
      ${o.fg}
      ${safe ? '<circle cx="54" cy="54" r="33" fill="none" stroke="#fff" stroke-opacity=".3" stroke-width="1" stroke-dasharray="3 3"/>' : ''}
    </svg>
    <span>${mask === 'sq' ? 'squircle' : mask === 'ro' ? 'tonda' : '48 dp'}</span>
  </div>`;

const html = `<!DOCTYPE html>
<meta charset="utf-8">
<title>Icone a confronto</title>
<style>
  body { margin:0; padding:24px; background:#15161c; color:#e8e8ee;
         font:13px "Segoe UI", system-ui, sans-serif; }
  h1 { font-size:15px; margin:0 0 4px; }
  p.note { color:#8e8e9c; margin:0 0 22px; max-width:1000px; line-height:1.5; }
  .grid { display:flex; gap:26px; align-items:flex-start; flex-wrap:wrap; }
  .col h2 { font-size:13px; font-weight:600; margin:0 0 2px; text-align:center; }
  .col .sub { font-size:11px; color:#8e8e9c; margin:0 0 10px; height:26px; text-align:center; }
  .row { display:flex; gap:10px; align-items:flex-end; }
  .cell { display:flex; flex-direction:column; align-items:center; gap:5px; }
  .cell span { font-size:10px; color:#6c6c7a; }
  svg { display:block; }
  .sq { clip-path: inset(0 round 23%); }
  .ro { clip-path: circle(50%); }
  .sm { clip-path: inset(0 round 23%); }
</style>
<h1>Icona di Been There — quella di adesso e quattro proposte</h1>
<p class="note">
  Le proposte usano i confini VERI di <code>countries.geojson</code>, proiettati
  in ortografica: gli stessi dati che l'app disegna sul globo. Per ognuna:
  maschera a squircle con la <b>zona sicura</b> tratteggiata, maschera tonda, e
  la dimensione reale sul telefono (48&nbsp;dp).
</p>
<div class="grid">
${OPTIONS.map(o => `
  <div class="col">
    <h2>${o.name}</h2><div class="sub">${o.sub}</div>
    <div class="row">${cell(o, 150, 'sq', true)}${cell(o, 150, 'ro', false)}${cell(o, 48, 'sm', false)}</div>
  </div>`).join('')}
</div>`;

writeFileSync('dev/icon-preview.html', html);
console.error('scritto dev/icon-preview.html');
