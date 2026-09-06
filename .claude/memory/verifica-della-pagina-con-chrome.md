---
name: verifica-della-pagina-con-chrome
description: "Come provare index.html da questa macchina: Chrome headless da riga di comando, e perche' per il ciclo di disegno serve un Chrome vero"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-06T00:00:00.000Z
---

L'estensione Claude-in-Chrome **non e' connessa** su questa macchina: gli
strumenti `mcp__claude-in-chrome__*` falliscono. Chrome pero' c'e', in
`C:\Program Files\Google\Chrome\Application\chrome.exe`, e basta da riga di
comando. Il globo si serve con `python -m http.server` dalla cartella
`app/src/main/assets` (da `file://` la fetch del GeoJSON e' bloccata).

**Screenshot e console:**

```
chrome.exe --headless=new --no-sandbox --enable-logging=stderr --v=0 \
  --virtual-time-budget=30000 --window-size=900,900 \
  "--screenshot=C:\percorso\assoluto\out.png" http://localhost:PORTA/index.html
```

Il percorso dello screenshot deve essere **assoluto in stile Windows**, altrimenti
"Access is denied". `--dump-dom` al posto di `--screenshot` stampa il DOM: e' la
via per far girare una pagina di prova che carica `index.html` in un iframe
(stessa origine, quindi `iframe.contentWindow.BeenThere` e' raggiungibile) e
scrive gli esiti in un `<pre>`.

**Trappola da ricordare:** con `--virtual-time-budget` Chrome **smette di
consegnare `requestAnimationFrame`** appena la pagina si quieta. Verificato: il
contatore rAF di una pagina di prova che non c'entra col globo si ferma a zero.
Quindi tutto cio' che dipende da frame continui - il ciclo di disegno che si
addormenta, il damping dei controlli, un'animazione - **non si puo' provare in
headless** e sembra rotto quando non lo e'. Per quello serve un Chrome vero:
lanciarlo con `--user-data-dir` in una cartella temporanea, far scrivere gli
esiti in `localStorage`, ucciderlo con `taskkill //IM chrome.exe //F` e
rileggere `localStorage` con un secondo giro headless **sullo stesso
`--user-data-dir`**.

Vedi [[ambiente-senza-toolchain-android]] e [[been-there-stato-lavoro]].
