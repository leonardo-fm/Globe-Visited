---
name: lavorare-a-fasi-verificabili
description: "L'utente vuole i lavori grossi divisi in fasi che puo' installare e provare una per una sul telefono"
metadata:
  node_type: memory
  type: feedback
  modified: 2026-09-07T00:00:00.000Z
---

Davanti a un piano grosso (2026-09-07, la funzione dei luoghi), l'utente ha
chiesto: *"Prima di iniziare il compito, cortesemente dividi in diverse sottofasi
che io possa verificare"*, e ha proposto lui stesso il taglio: prima le citta' si
cercano, poi si accendono sul globo, poi i pin personali.

**Perche' conta:** non puo' compilare da questa macchina - il collaudo lo fa lui
sul suo S10e (vedi [[ambiente-senza-toolchain-android]]). Una consegna unica e
grossa lo costringerebbe a provare tutto insieme e a non sapere quale pezzo ha
rotto cosa. Il taglio che ha proposto e' per **funzione visibile**, non per
strato tecnico: non "prima il modello dati, poi la UI", ma "prima riesco a
cercare le citta'".

**Come applicarlo:** proporre il taglio in fasi *dentro il piano*, prima che lo
chieda. Ogni fase deve finire con un'app che compila, si installa e fa qualcosa
di provabile a mano, e il piano deve dire in fondo a ogni fase **cosa deve
provare lui**. Non iniziare la fase successiva finche' non ha confermato la
precedente.

Vedi [[utente-vuole-compromessi-espliciti]] e [[been-there-stato-lavoro]].
