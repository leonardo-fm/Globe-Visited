---
name: been-there-stato-lavoro
description: "Punto in cui si e' fermato il lavoro su Been There al 2026-09-06: l'app gira sul telefono ed e' stato riscritto il rendering"
metadata: 
  node_type: memory
  type: project
  originSessionId: 41cb3d5f-b93c-4509-833e-f6ccfe5d5cb4
  modified: 2026-09-06T00:00:00.000Z
---

Al 2026-09-05 "Been There" **compila e gira sul telefono**: l'utente ha
generato il wrapper Gradle, scaricato gli asset e fatto il primo
`assembleDebug` da solo. Da allora il lavoro e' su bug e rifiniture trovati
usando l'app.

**2026-09-06: riscritto il rendering di `index.html`** per il framerate su un
Samsung S10e e per lo sfarfallio dei confini. In sintesi: le 470 calotte fuse in
una mesh sola con colore per-vertice, i confini in una linea sola deduplicata,
il tocco spostato dal raycast a un point-in-polygon, piani di taglio stretti a
ogni movimento di camera, disegno su richiesta e risoluzione adattiva. Da ~1400
draw call a **4**. Il perche' di ogni scelta sta in `docs/note-tecniche.md` — non
riassumerlo qui, e' li' che va letto prima di toccare il rendering.

**Non verificato sul telefono.** Le prove sono state fatte con Chrome sul PC
(vedi [[verifica-della-pagina-con-chrome]]): geometria, tocco, colori, contorni,
piani di taglio e ciclo di disegno passano, ma i numeri di framerate sull'S10e
li puo' dare solo l'utente. Se qualcosa non torna, i due sospettati indicati
nelle note sono `CAP_SIDE` (le calotte spariscono) e la soglia di
`sampleFrame()` (la risoluzione scende quando non dovrebbe).

L'utente prova lui: chiede esplicitamente di non far girare l'app da qui.
La verifica possibile da questa macchina resta quella descritta in
[[ambiente-senza-toolchain-android]].

Vedi [[been-there-decisioni-prodotto]].
