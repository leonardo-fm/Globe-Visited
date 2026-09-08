---
name: banco-di-prova-web
description: "dev/harness.html: come si prova nel browser la app intera, e cosa quella prova NON dimostra"
metadata:
  node_type: memory
  type: project
  modified: 2026-09-08T00:00:00.000Z
---

`dev/harness.html` (2026-09-08, chiesto dall'utente per provare prima nel
browser e poi sul telefono). Sta **fuori** da `app/src/main/assets` apposta:
tutto cio' che sta in assets finisce nell'APK.

**Come si lancia:** il server va avviato dalla **radice del repo**, non dagli
assets - `python -m http.server 8000` in `D:\Projects\Globe-Visited`, poi
`http://localhost:8000/dev/harness.html`. Serve cosi' perche' il banco legge
anche `app/src/main/res/values*/strings.xml`.

**Come funziona.** Aprendo `index.html` nel browser da solo, la pagina non
trova il ponte (`HAS_NATIVE`, ~riga 439) e accende un **prototipo suo**: barra
di ricerca HTML, citta' caricate dalla pagina, stato in localStorage. Non e' il
ramo che gira sul telefono. Il banco carica `index.html` con `fetch`, ci
inietta davanti `<base>` + `window.AndroidBridge = parent.__bridge` e lo mette
in un iframe con `srcdoc`: l'iniezione **deve** precedere gli script della
pagina, perche' `HAS_NATIVE` si legge una volta sola all'avvio. Cosi' il
prototipo resta spento e il globo si comporta come nella WebView; la parte
Compose (contatore, ricerca, sheet, dialoghi, backup) la rifa' il banco in
HTML, con le stesse regole di `ui/*.kt` e le stringhe lette dalle stesse
`res/values`.

**Cosa non dimostra:** e' una seconda implementazione. I difetti nati da
Compose - il fuoco che passa alla WebView, lo spazio fra due righe di testo -
qui non compaiono. Vanno provati sull'S10e, vedi
[[ambiente-senza-toolchain-android]].

**Verificato il 2026-09-08** in Chrome headless: `onGlobeReady` arriva (21.310
byte di catalogo), 4.205 citta' agganciate, contatore `0 / 242`, ricerca paesi
e citta' con accenti, stato vuoto con "Crea un luogo", tocco sulla barra che
azzera, pallino che accende il visitato, sheet con backup e lingua.

Vedi [[verifica-della-pagina-con-chrome]] e [[been-there-stato-lavoro]].
