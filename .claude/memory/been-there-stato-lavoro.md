---
name: been-there-stato-lavoro
description: "Punto in cui si e' fermato il lavoro su Been There al 2026-09-05: l'app gira sul telefono, cosa e' stato corretto dopo il primo build"
metadata: 
  node_type: memory
  type: project
  originSessionId: 41cb3d5f-b93c-4509-833e-f6ccfe5d5cb4
  modified: 2026-09-05T00:00:00.000Z
---

Al 2026-09-05 "Been There" **compila e gira sul telefono**: l'utente ha
generato il wrapper Gradle, scaricato gli asset e fatto il primo
`assembleDebug` da solo. I tre punti aperti del 2026-09-04 sono chiusi.

Da allora il lavoro e' su bug e rifiniture trovati usando l'app:

- **Verso degli anelli del GeoJSON** (commit `a10e0fd`, "fixed bug state
  orange"): il dataset e' RFC 7946 mentre d3-geo vuole la convenzione opposta,
  quindi ogni paese veniva reso come il proprio complemento - colore fuori dai
  confini e tocco che selezionava il vicino. `index.html` ora riavvolge i
  poligoni all'avvio. Causa e catena completa stanno nel README.
- **Popup del paese selezionato**: tocco = selezione, non piu' toggle. Vedi
  [[been-there-decisioni-prodotto]] per le scelte concordate.

L'utente prova lui: chiede esplicitamente di non far girare l'app da qui.
La verifica possibile da questa macchina resta quella descritta in
[[ambiente-senza-toolchain-android]].
