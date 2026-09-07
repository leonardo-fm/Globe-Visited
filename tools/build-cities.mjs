// Genera app/src/main/assets/cities.json da Natural Earth 1:10m populated places.
//
//   curl -O https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_populated_places.geojson
//   node tools/build-cities.mjs ne_10m_populated_places.geojson app/src/main/assets/cities.json
//
// Il perche' delle scelte sta in docs/note-tecniche.md, sezione "Il catalogo
// delle citta'". In sintesi: si tengono capitali di stato, capoluoghi di regione
// e tutto quello che supera i 100.000 abitanti; l'ordine del file e' per
// popolazione decrescente, perche' la ricerca lo conserva a parita' di match.

import { readFileSync, writeFileSync } from 'node:fs';

const [, , inPath, outPath] = process.argv;
if (!inPath || !outPath) {
  console.error('uso: node tools/build-cities.mjs <ne_10m_populated_places.geojson> <cities.json>');
  process.exit(1);
}

const POP_MIN = 100000;

const raw = JSON.parse(readFileSync(inPath, 'utf8'));

// NAMEASCII e' gia' ASCII, ma non e' garantito privo di apostrofi e spazi:
// l'id deve restare stabile e stampabile, non deve essere bello.
function slug(value) {
  return String(value || '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

function round3(n) {
  return Math.round(n * 1000) / 1000;
}

const kept = [];
const skipped = { noCode: 0, noName: 0, tooSmall: 0 };

for (const feature of raw.features) {
  const p = feature.properties || {};
  const isCapital = /capital/i.test(p.FEATURECLA || '');
  const pop = Number(p.POP_MAX) || 0;
  if (!isCapital && pop < POP_MIN) { skipped.tooSmall++; continue; }

  const nameEn = p.NAME_EN || p.NAME || p.NAMEASCII;
  if (!nameEn) { skipped.noName++; continue; }

  // Il codice del paese serve ad agganciare la citta' alla feature del globo.
  // Senza, la citta' resterebbe senza bandiera e senza paese nella ricerca.
  const code = p.ADM0_A3;
  if (!code || code === '-99') { skipped.noCode++; continue; }

  const coords = (feature.geometry && feature.geometry.coordinates) || [];
  const lng = Number(coords[0] ?? p.LONGITUDE);
  const lat = Number(coords[1] ?? p.LATITUDE);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) { skipped.noName++; continue; }

  kept.push({
    slug: slug(p.NAMEASCII || nameEn),
    a3: code,
    n: nameEn,
    t: p.NAME_IT || nameEn,
    y: round3(lat),
    x: round3(lng),
    pop
  });
}

// Ordine per popolazione decrescente: la ricerca lo conserva a parita' di
// match, quindi "New York" esce prima di "New York Mills".
kept.sort((a, b) => b.pop - a.pop || a.n.localeCompare(b.n));

// Id stabile fra rigenerazioni: <ADM0_A3>:<nome ascii>. In caso di omonimia
// nello stesso paese si aggiunge un contatore, assegnato nell'ordine appena
// fissato, che non dipende dall'ordine del file di partenza.
const used = new Map();
const out = [];
for (const c of kept) {
  const base = c.a3 + ':' + (c.slug || 'x');
  const seen = used.get(base) || 0;
  used.set(base, seen + 1);
  const id = seen === 0 ? base : base + '-' + (seen + 1);
  const entry = { i: id, n: c.n, a: c.a3, y: c.y, x: c.x };
  // Il nome italiano si scrive solo quando differisce: su ~4.000 voci la
  // stragrande maggioranza coincide con l'inglese, e ripeterla raddoppierebbe
  // il peso del campo per niente. Chi legge ricade su `n`.
  if (c.t && c.t !== c.n) entry.t = c.t;
  out.push(entry);
}

writeFileSync(outPath, JSON.stringify(out), 'utf8');

const withIt = out.filter(c => c.t).length;
const dupes = [...used.values()].filter(n => n > 1).length;
console.log('citta tenute      : ' + out.length + ' su ' + raw.features.length);
console.log('scartate          : ' + skipped.tooSmall + ' sotto i ' + POP_MIN +
            ' abitanti e non capoluoghi, ' + skipped.noCode + ' senza ADM0_A3, ' +
            skipped.noName + ' senza nome o coordinate');
console.log('nome IT diverso   : ' + withIt);
console.log('id con omonimia   : ' + dupes);
console.log('paesi distinti    : ' + new Set(out.map(c => c.a)).size);
