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

**Confermato dall'utente sul suo Samsung S10e il 2026-09-06**: framerate e
confini a posto, "funziona tutto". Le prove automatiche erano state fatte con
Chrome sul PC (vedi [[verifica-della-pagina-con-chrome]]).

**2026-09-06, secondo giro: alzata la risoluzione del dataset** a
`simplify 40% / precision 0.001`. Vertici da 9.708 a 41.128, poligoni da 470 a
1.359 (tornano gli arcipelaghi: le Hawaii passano da 2 a 7 isole). Zero modifiche
al codice di rendering: deduplica, tassellatura, tocco e contorni si dimensionano
sul file. Aggiunta una schermata di caricamento con globo wireframe, perche' la
costruzione e' passata da ~0,85 a ~2,0 secondi sul PC. Tolto `noCompress` dal
geojson. Il denominatore del contatore resta 242, quindi nessuna decisione di
prodotto e' stata riaperta.

Non provato sul telefono: mancano il tempo di avvio reale sull'S10e e la conferma
che il wireframe continui a girare mentre il thread e' bloccato.

L'utente prova lui: chiede esplicitamente di non far girare l'app da qui.
La verifica possibile da questa macchina resta quella descritta in
[[ambiente-senza-toolchain-android]].

Vedi [[been-there-decisioni-prodotto]].
