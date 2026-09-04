---
name: been-there-decisioni-prodotto
description: "Decisioni di prodotto prese con l'utente su Been There e perche', comprese quelle scartate"
metadata: 
  node_type: memory
  type: project
  originSessionId: 41cb3d5f-b93c-4509-833e-f6ccfe5d5cb4
  modified: 2026-09-04T19:08:09.795Z
---

Decisioni concordate con l'utente il 2026-09-04, che si discostano dalla sua
specifica iniziale e vanno rispettate senza ridiscuterle:

- **Dataset Natural Earth 50m, non 110m.** A 1:110m Malta, Singapore e Maldive
  non esistono come poligoni: non sarebbero ne' cliccabili ne' cercabili.
- **Contatore con denominatore dinamico** (~242 poligoni), non "195": nessun
  dataset NE coincide con la lista ONU. Territori non sovrani cliccabili e
  contati singolarmente, Antartide inclusa e cliccabile.
- **WebViewAssetLoader** su `https://appassets.androidplatform.net/`, non
  `file:///android_asset/` come da specifica iniziale: da origine file:// la
  WebView blocca la fetch del GeoJSON. Il permesso INTERNET resta non dichiarato.
- **Lingua**: selettore manuale IT/EN nella bottom sheet, default italiano,
  cambia nomi dei paesi e stringhe UI. Non segue il locale di sistema.
- **Ricerca**: tocco sulla riga = vola sul paese, pallino separato = toggle
  visitato. Il pallino e' l'unica via di selezione per gli stati insulari
  minuscoli, quindi non e' un extra rimovibile.
- **Unica funzionalita' extra concessa**: la lista dei visitati in bottom sheet
  aperta dal contatore. Niente reset globale, niente export/import.

Il piano completo, con architettura e passi di verifica, sta nel repo in
`.claude/plans/piano-progetto-android.md`.

Vedi [[been-there-stato-lavoro]] e [[utente-vuole-compromessi-espliciti]].
