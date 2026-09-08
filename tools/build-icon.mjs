/**
 * Genera il disegno del globo per l'icona, proiettando i confini VERI di
 * countries.geojson - gli stessi che l'app disegna - invece di macchie
 * inventate a mano.
 *
 * Proiezione ortografica: e' come si vede una sfera da lontano, ed e' la stessa
 * cosa che fa il globo 3D quando lo guardi fermo. L'emisfero nascosto si butta.
 *
 * Uso:
 *   node tools/build-icon.mjs --lon 10 --lat 20 --accent ITA
 *
 * Stampa i <path> pronti da incollare in un vector drawable, nel sistema di
 * coordinate 108x108 che Android usa per le icone adattive.
 */

import { readFileSync } from 'node:fs';

const args = Object.fromEntries(
  process.argv.slice(2).reduce((acc, cur, i, a) => {
    if (cur.startsWith('--')) acc.push([cur.slice(2), a[i + 1]]);
    return acc;
  }, [])
);

const LON0 = Number(args.lon ?? 10);      // centro della vista, gradi est
const LAT0 = Number(args.lat ?? 20);      // centro della vista, gradi nord
const ACCENT = (args.accent ?? '').split(',').filter(Boolean);
const R = Number(args.r ?? 34);           // raggio della sfera nel viewport 108
const CX = 54, CY = 54;
const TOL = Number(args.tol ?? 0.25);     // semplificazione, in unita' del viewport
const MIN_AREA = Number(args.minarea ?? 0.15);  // sotto questa area il pezzo non si vede

const geo = JSON.parse(readFileSync('app/src/main/assets/countries.geojson', 'utf8'));

const rad = (d) => (d * Math.PI) / 180;
const sinLat0 = Math.sin(rad(LAT0));
const cosLat0 = Math.cos(rad(LAT0));

/** Ortografica. Torna null se il punto sta sull'altra faccia della Terra. */
function project([lon, lat]) {
  const phi = rad(lat);
  const lambda = rad(lon - LON0);
  // coseno della distanza angolare dal centro: negativo = emisfero nascosto
  const cosC = sinLat0 * Math.sin(phi) + cosLat0 * Math.cos(phi) * Math.cos(lambda);
  if (cosC < 0) return null;
  const x = CX + R * Math.cos(phi) * Math.sin(lambda);
  const y = CY - R * (cosLat0 * Math.sin(phi) - sinLat0 * Math.cos(phi) * Math.cos(lambda));
  return [x, y];
}

/**
 * Semplifica un tracciato, chiuso o aperto.
 *
 * Su un anello chiuso Douglas-Peucker da solo NON funziona: primo e ultimo
 * punto coincidono, quindi il segmento di riferimento ha lunghezza zero, tutte
 * le distanze risultano zero e la forma collassa a due punti. Si spezza prima
 * l'anello nel punto piu' lontano dall'inizio, ottenendo due tracciati aperti.
 */
function simplifyRing(pts, tol) {
  const closed = pts.length > 2 &&
    Math.abs(pts[0][0] - pts[pts.length - 1][0]) < 1e-9 &&
    Math.abs(pts[0][1] - pts[pts.length - 1][1]) < 1e-9;
  if (!closed) return simplify(pts, tol);

  const ring = pts.slice(0, -1);
  if (ring.length < 4) return ring;
  let far = 0, farD = -1;
  for (let i = 1; i < ring.length; i++) {
    const d = Math.hypot(ring[i][0] - ring[0][0], ring[i][1] - ring[0][1]);
    if (d > farD) { farD = d; far = i; }
  }
  const a = simplify(ring.slice(0, far + 1), tol);
  const b = simplify(ring.slice(far).concat([ring[0]]), tol);
  return a.slice(0, -1).concat(b.slice(0, -1));
}

/** Ramer-Douglas-Peucker: toglie i punti che non cambiano la forma. */
function simplify(pts, tol) {
  if (pts.length < 3) return pts;
  let maxD = 0, idx = 0;
  const [ax, ay] = pts[0], [bx, by] = pts[pts.length - 1];
  const dx = bx - ax, dy = by - ay;
  const len = Math.hypot(dx, dy) || 1;
  for (let i = 1; i < pts.length - 1; i++) {
    const [px, py] = pts[i];
    const d = Math.abs((px - ax) * dy - (py - ay) * dx) / len;
    if (d > maxD) { maxD = d; idx = i; }
  }
  if (maxD <= tol) return [pts[0], pts[pts.length - 1]];
  return [
    ...simplify(pts.slice(0, idx + 1), tol).slice(0, -1),
    ...simplify(pts.slice(idx), tol)
  ];
}

const round = (n) => Math.round(n * 10) / 10;

/** Un anello di coordinate -> un pezzo di path, o '' se non si vede o e' minuscolo. */
function ringToPath(ring, minArea) {
  // Spezza dove il contorno passa dietro la sfera: due tratti separati, non uno
  // solo che taglierebbe dritto attraverso il pianeta.
  const runs = [];
  let run = [];
  for (const coord of ring) {
    const p = project(coord);
    if (p) { run.push(p); }
    else if (run.length) { runs.push(run); run = []; }
  }
  if (run.length) runs.push(run);

  let out = '';
  for (const r of runs) {
    const s = simplifyRing(r, TOL);
    if (s.length < 3) continue;
    // area: le schegge sotto mezzo pixel quadrato non si vedono e pesano
    let area = 0;
    for (let i = 0; i < s.length; i++) {
      const [x1, y1] = s[i], [x2, y2] = s[(i + 1) % s.length];
      area += x1 * y2 - x2 * y1;
    }
    if (Math.abs(area / 2) < minArea) continue;
    out += 'M' + s.map(([x, y]) => `${round(x)},${round(y)}`).join('L') + 'Z';
  }
  return out;
}

function featurePath(feature, minArea) {
  const g = feature.geometry;
  const polys = g.type === 'Polygon' ? [g.coordinates] : g.coordinates;
  let d = '';
  for (const poly of polys) d += ringToPath(poly[0], minArea);   // solo il contorno esterno
  return d;
}

const key = (f) => f.properties.ISO_A3 !== '-99' ? f.properties.ISO_A3 : f.properties.ADM0_A3;

let land = '', accent = '';
for (const f of geo.features) {
  const isAccent = ACCENT.includes(key(f));
  // Il paese acceso si disegna sempre, anche se e' piccolo: e' il soggetto.
  const d = featurePath(f, isAccent ? 0 : MIN_AREA);
  if (!d) continue;
  if (isAccent) accent += d; else land += d;
}

const stat = (s) => `${s.length} caratteri, ${(s.match(/M/g) || []).length} pezzi`;
console.error(`centro ${LON0}E ${LAT0}N, raggio ${R}, tolleranza ${TOL}`);
console.error(`  terre:    ${stat(land)}`);
console.error(`  accento:  ${accent ? stat(accent) : '(nessuno)'}  [${ACCENT.join(',') || '-'}]`);

// --------------------------------------------------------------- i drawable
// Con --write i file dell'icona si riscrivono davvero. Senza, si stampa il
// JSON e basta: e' quello che usa dev/make-icon-preview.mjs.
if (!args.write) {
  console.log(JSON.stringify({ land, accent, r: R, cx: CX, cy: CY }));
  process.exit(0);
}

const OCEAN = '#1B4B8F', LAND_C = '#8A8A8A', VISITED_C = '#FF8C1A', SPACE = '#05070F';

/** Il cerchio della sfera come pathData: due archi, come vuole il formato. */
const circlePath = (r) =>
  `M${CX},${CY - r}a${r},${r} 0 1,0 0,${2 * r}a${r},${r} 0 1,0 0,${-2 * r}Z`;

// Attenzione: dentro un commento XML la sequenza di due trattini e' vietata,
// quindi le opzioni si scrivono senza, con la nota su come rimetterli.
const header = (extraNs = '') =>
  `<?xml version="1.0" encoding="utf-8"?>\n` +
  `<!-- GENERATO da tools/build-icon.mjs: non modificare a mano.\n` +
  `     Per rifarlo, rilancia lo script con queste opzioni (ognuna preceduta\n` +
  `     da due trattini, che qui non si possono scrivere):\n` +
  `       lon ${LON0} | lat ${LAT0} | r ${R} | tol ${TOL} | minarea ${MIN_AREA}\n` +
  `       accent ${ACCENT.join(',')} | write 1 -->\n` +
  `<vector xmlns:android="http://schemas.android.com/apk/res/android"${extraNs}\n` +
  `    android:width="108dp"\n` +
  `    android:height="108dp"\n` +
  `    android:viewportWidth="108"\n` +
  `    android:viewportHeight="108">\n`;

// Il primo piano: sfera oceano sfumata, terre grigie, paesi visitati arancioni.
// La sfumatura radiale non e' vezzo: senza, il disco resta piatto e a 48dp
// sembra un bollino invece che un pianeta.
const foreground = header(`\n    xmlns:aapt="http://schemas.android.com/aapt"`) +
`
    <!-- oceano -->
    <path android:pathData="${circlePath(R)}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="${CX - R * 0.28}"
                android:centerY="${CY - R * 0.34}"
                android:gradientRadius="${R * 1.55}">
                <item android:offset="0" android:color="#2F6DC0"/>
                <item android:offset="0.65" android:color="${OCEAN}"/>
                <item android:offset="1" android:color="#0F2A4A"/>
            </gradient>
        </aapt:attr>
    </path>

    <!-- Ritagliato sulla sfera: la semplificazione puo' spingere un tratto
         qualche decimo fuori dal bordo, e si vedrebbe. -->
    <group>
        <clip-path android:pathData="${circlePath(R)}"/>
        <path
            android:pathData="${land}"
            android:fillColor="${LAND_C}"/>
        <path
            android:pathData="${accent}"
            android:fillColor="${VISITED_C}"/>
    </group>
</vector>
`;

// Il livello monocromatico (icone a tema, Android 13+). Va disegnato a parte:
// il sistema lo tinge tutto di un colore solo, quindi riusare il primo piano
// a colori darebbe un disco pieno e uniforme, con l'arancione sparito e
// l'idea del globo con lui. Qui restano un anello e le terre, che tinti di
// qualunque colore continuano a leggersi come un pianeta.
const monochrome = header() +
`
    <path
        android:pathData="${circlePath(R - 1.5)}"
        android:strokeColor="#000000"
        android:strokeWidth="3"
        android:fillColor="#00000000"/>
    <group>
        <clip-path android:pathData="${circlePath(R - 3)}"/>
        <path android:pathData="${land}" android:fillColor="#000000"/>
        <path android:pathData="${accent}" android:fillColor="#000000"/>
    </group>
</vector>
`;

// Il fondo. NON un colore piatto: #05070F e' quasi nero, e su uno sfondo
// scuro la piastrella dell'icona sparisce lasciando il globo a mezz'aria.
// Una sfumatura radiale appena accennata - blu notte al centro, nero dell'app
// ai bordi - ridisegna il contorno dell'icona senza smettere di essere spazio.
const backgroundDrawable = header(`\n    xmlns:aapt="http://schemas.android.com/aapt"`) +
`
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="radial"
                android:centerX="54"
                android:centerY="54"
                android:gradientRadius="76">
                <item android:offset="0" android:color="#1B2E52"/>
                <item android:offset="0.55" android:color="#0C1428"/>
                <item android:offset="1" android:color="${SPACE}"/>
            </gradient>
        </aapt:attr>
    </path>
</vector>
`;

// Resta anche come colore: lo usa il tema per lo sfondo di avvio.
const background =
`<?xml version="1.0" encoding="utf-8"?>
<!-- GENERATO da tools/build-icon.mjs -->
<resources>
    <!-- Lo spazio dietro il globo: lo stesso nero su cui l'app disegna. -->
    <color name="ic_launcher_background">${SPACE}</color>
</resources>
`;

const { writeFileSync } = await import('node:fs');
const files = [
  ['app/src/main/res/drawable/ic_launcher_foreground.xml', foreground],
  ['app/src/main/res/drawable/ic_launcher_monochrome.xml', monochrome],
  ['app/src/main/res/drawable/ic_launcher_background.xml', backgroundDrawable],
  ['app/src/main/res/values/ic_launcher_background.xml', background]
];
for (const [path, content] of files) {
  writeFileSync(path, content);
  console.error(`  scritto ${path}  (${(content.length / 1024).toFixed(1)} KB)`);
}
