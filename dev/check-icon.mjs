/**
 * Rilegge i drawable VERI dell'icona e li ridisegna come SVG, per guardare
 * quello che finisce nell'APK invece di un disegno parallelo fatto apposta.
 *
 * node dev/check-icon.mjs   ->  dev/icon-check.html
 */
import { readFileSync, writeFileSync } from 'node:fs';

const RES = 'app/src/main/res/';

function attr(xml, name) {
  const m = new RegExp(`${name}="([^"]*)"`).exec(xml);
  return m ? m[1] : null;
}

/** Estrae dal vector drawable quello che serve a ridisegnarlo. */
function parseVector(file) {
  const xml = readFileSync(RES + file, 'utf8');

  const paths = [];
  // ogni <path ...> con il suo pathData e il riempimento
  const re = /<path\b([\s\S]*?)(?:\/>|<\/path>)/g;
  let m;
  while ((m = re.exec(xml))) {
    const body = m[1] + (m[0].includes('</path>') ? m[0] : '');
    paths.push({
      d: attr(body, 'android:pathData'),
      fill: attr(body, 'android:fillColor'),
      stroke: attr(body, 'android:strokeColor'),
      strokeWidth: attr(body, 'android:strokeWidth'),
      gradient: /<gradient/.test(m[0]) ? {
        cx: attr(m[0], 'android:centerX'),
        cy: attr(m[0], 'android:centerY'),
        r: attr(m[0], 'android:gradientRadius'),
        stops: [...m[0].matchAll(/<item android:offset="([^"]*)" android:color="([^"]*)"\/>/g)]
          .map((s) => ({ offset: s[1], color: s[2] }))
      } : null
    });
  }
  const clip = attr(xml, 'android:pathData\\s*"?');
  const clipM = /<clip-path android:pathData="([^"]*)"/.exec(xml);
  return { paths, clip: clipM ? clipM[1] : null, raw: xml };
}

const fg = parseVector('drawable/ic_launcher_foreground.xml');
const mono = parseVector('drawable/ic_launcher_monochrome.xml');
// Il fondo ora e' un drawable con la sua sfumatura, non piu' un colore piatto.
const bgV = parseVector('drawable/ic_launcher_background.xml');
const BG = 'none';
const bgSvg = toSvg(bgV);

function toSvg(v, forceColor) {
  let defs = '', body = '';
  const clipId = 'clip' + Math.random().toString(36).slice(2, 7);
  if (v.clip) defs += `<clipPath id="${clipId}"><path d="${v.clip}"/></clipPath>`;

  v.paths.forEach((p, i) => {
    if (!p.d) return;
    // il primo path (la sfera) sta fuori dal group: non va ritagliato
    const clipped = i > 0 && v.clip ? ` clip-path="url(#${clipId})"` : '';
    let fill = forceColor || p.fill || 'none';
    if (fill === '#00000000') fill = 'none';
    if (p.gradient) {
      const gid = 'g' + i + Math.random().toString(36).slice(2, 6);
      defs += `<radialGradient id="${gid}" gradientUnits="userSpaceOnUse" ` +
        `cx="${p.gradient.cx}" cy="${p.gradient.cy}" r="${p.gradient.r}">` +
        p.gradient.stops.map((s) =>
          `<stop offset="${s.offset}" stop-color="${s.color}"/>`).join('') +
        `</radialGradient>`;
      fill = `url(#${gid})`;
    }
    const stroke = p.stroke
      ? ` stroke="${forceColor || p.stroke}" stroke-width="${p.strokeWidth || 1}"` : '';
    body += `<path d="${p.d}" fill="${fill}"${stroke}${clipped}/>`;
  });
  return `<defs>${defs}</defs>${body}`;
}

const fgSvg = toSvg(fg);
const monoSvg = toSvg(mono, null);

const cell = (inner, bg, size, mask, label) => `
  <div class="cell">
    <svg width="${size}" height="${size}" viewBox="0 0 108 108" class="${mask}">
      <rect width="108" height="108" fill="${bg}"/>${inner}
    </svg>
    <span>${label}</span>
  </div>`;

// Il vecchio disegno, per il confronto: e' quello che c'era prima di oggi.
const OLD = `
  <circle cx="54" cy="54" r="24" fill="#8A8A8A"/>
  <path d="M30,54L78,54" stroke="#05070F" stroke-width="2" stroke-opacity=".55"/>
  <ellipse cx="54" cy="54" rx="13" ry="24" fill="none" stroke="#05070F" stroke-width="2" stroke-opacity=".55"/>
  <circle cx="70" cy="43" r="7" fill="#FF8C1A"/>`;

const html = `<!DOCTYPE html>
<meta charset="utf-8"><title>Verifica icona</title>
<style>
  body{margin:0;padding:24px;background:#15161c;color:#e8e8ee;
       font:13px "Segoe UI",system-ui,sans-serif}
  h1{font-size:15px;margin:0 0 4px}
  p{color:#8e8e9c;margin:0 0 20px;max-width:900px;line-height:1.5}
  .grid{display:flex;gap:34px;align-items:flex-start}
  .col h2{font-size:13px;margin:0 0 10px;text-align:center}
  .row{display:flex;gap:10px;align-items:flex-end}
  .cell{display:flex;flex-direction:column;align-items:center;gap:5px}
  .cell span{font-size:10px;color:#6c6c7a}
  svg{display:block}
  .sq{clip-path:inset(0 round 23%)} .ro{clip-path:circle(50%)}
</style>
<h1>Icona — riletta dai file che finiscono nell'APK</h1>
<p>
  Questo disegno NON e' rifatto a mano: e' <code>ic_launcher_foreground.xml</code>
  e <code>ic_launcher_monochrome.xml</code> riletti dal disco e ridisegnati. Se
  qui si vede bene, i file sono giusti.
</p>
<div class="grid">
  <div class="col"><h2>Prima</h2><div class="row">
    ${cell(OLD, '#1B4B8F', 150, 'sq', 'squircle')}
    ${cell(OLD, '#1B4B8F', 48, 'sq', '48 dp')}
  </div></div>
  <div class="col"><h2>Dopo</h2><div class="row">
    ${cell(bgSvg + fgSvg, BG, 150, 'sq', 'squircle')}
    ${cell(bgSvg + fgSvg, BG, 150, 'ro', 'tonda')}
    ${cell(bgSvg + fgSvg, BG, 48, 'sq', '48 dp')}
  </div></div>
  <div class="col"><h2>A tema (Android 13+)</h2><div class="row">
    ${cell(monoSvg, '#C8D6E8', 150, 'sq', 'tinta chiara')}
    ${cell(monoSvg, '#C8D6E8', 48, 'sq', '48 dp')}
  </div></div>
</div>`;

writeFileSync('dev/icon-check.html', html);
console.error(`primo piano: ${fg.paths.length} path, clip ${fg.clip ? 'si' : 'NO'}`);
console.error(`monocromo:   ${mono.paths.length} path`);
console.error(`fondo:       ${BG}`);
console.error('scritto dev/icon-check.html');
