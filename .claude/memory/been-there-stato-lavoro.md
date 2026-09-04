---
name: been-there-stato-lavoro
description: "Punto in cui si e' fermato il lavoro su Been There al 2026-09-04 e cosa manca per compilare"
metadata: 
  node_type: memory
  type: project
  originSessionId: 41cb3d5f-b93c-4509-833e-f6ccfe5d5cb4
  modified: 2026-09-04T19:07:57.688Z
---

Al 2026-09-04 il progetto Android "Been There" (globo 3D dei paesi visitati) e'
scritto per intero ma **non e' mai stato compilato**: fase 1 (index.html
autonomo) e fase 2 (guscio Kotlin/Compose) sono entrambe consegnate.

Mancano tre cose, tutte a carico dell'utente:
1. scaricare `globe.gl.min.js` e `countries.geojson` in `app/src/main/assets/`
   (URL e comando mapshaper stanno nel README del repo);
2. generare `gradle-wrapper.jar` con `gradle wrapper` o aprendo il progetto in
   Android Studio — e' un binario che non posso produrre io;
3. il primo `assembleDebug`, che e' anche la prima vera verifica del codice.

L'utente non ha mai confermato di aver provato `index.html` nel browser: la
fase 2 e' stata costruita su quel presupposto.

Vedi [[been-there-decisioni-prodotto]] e [[ambiente-senza-toolchain-android]].
