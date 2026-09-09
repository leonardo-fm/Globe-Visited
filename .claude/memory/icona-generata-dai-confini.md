---
name: icona-generata-dai-confini
description: "L'icona dell'app e' generata da tools/build-icon.mjs proiettando countries.geojson: i drawable non si modificano a mano"
metadata:
  node_type: memory
  type: project
  modified: 2026-09-09T00:00:00.000Z
---

Dal 2026-09-08 l'icona non e' piu' disegnata a mano. `tools/build-icon.mjs`
prende i confini veri di `app/src/main/assets/countries.geojson` - gli stessi
che l'app disegna - li proietta in **ortografica**, li semplifica con
Douglas-Peucker e scrive tre file:

- `res/drawable/ic_launcher_foreground.xml` — sfera oceano sfumata, terre
  grigie, paesi visitati arancioni, ritagliati sulla sfera
- `res/drawable/ic_launcher_background.xml` — sfumatura radiale blu notte
- `res/drawable/ic_launcher_monochrome.xml` — anello + terre, per le icone a
  tema di Android 13+

**Portano tutti un'intestazione "GENERATO": non vanno modificati a mano**, si
rilancia lo script. Quello di adesso:

```
node tools/build-icon.mjs --lon 10 --lat 25 --r 26.25 --tol 0.45 \
  --minarea 0.35 --accent ITA,FRA,ESP,PRT,GRC,MAR,EGY,TUR,HRV --write 1
```

`dev/check-icon.mjs` rilegge i drawable **dal disco** e li ridisegna in
`dev/icon-check.html`: e' il modo di guardare quello che finisce nell'APK invece
di un SVG parallelo.

**Quattro trappole gia' pagate, da non ripetere:**

1. **Douglas-Peucker collassa gli anelli chiusi.** Primo e ultimo punto
   coincidono, il segmento di riferimento ha lunghezza zero, tutte le distanze
   risultano zero e la forma si riduce a due punti. Sopravvivevano solo le
   sagome tagliate dall'orizzonte. Si spezza l'anello nel punto piu' lontano
   dall'inizio e si semplificano i due tratti aperti (`simplifyRing`).
2. **Dentro un commento XML due trattini di fila sono vietati**: scrivere
   `--lon` nell'intestazione generata fa fallire `mergeDebugResources`.
3. **Il livello monocromatico va disegnato a parte.** Puntarlo al primo piano a
   colori da' un disco piatto e uniforme: il sistema lo tinge tutto di un
   colore solo e l'arancione sparisce con l'idea del globo.
4. **Il fondo non puo' essere `#05070F` piatto**: e' quasi nero, e su uno
   sfondo scuro la piastrella sparisce lasciando il globo a mezz'aria. Serve
   una sfumatura che ne ridisegni il contorno.

Un paese solo non si vede a 48 dp - l'Italia e' un puntino, lo script la
scartava da sola per area sotto soglia - quindi se ne accendono diversi.

Vedi [[been-there-stato-lavoro]] e [[verifica-della-pagina-con-chrome]].
