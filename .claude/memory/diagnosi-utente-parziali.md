---
name: diagnosi-utente-parziali
description: "L'utente segnala i difetti con una causa gia' abbozzata: le sue diagnosi colgono nel segno ma raramente esauriscono il problema"
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-09-06T00:00:00.000Z
---

Quando segnala un difetto, l'utente ci allega la sua ipotesi sulla causa, e
l'ipotesi e' quasi sempre **giusta ma parziale**.

Esempio del 2026-09-06, sfarfallio dei confini: "essendo che sono 2 cose una
sopra l'altra... trovarai un modo per evitare di disegnare due connessioni nello
stesso punto". Verissimo - 2143 spigoli su 9235 erano disegnati due volte - ma
era meta' della storia. Le altre due cause erano i muri laterali trasparenti che
scrivevano comunque nel depth buffer, e soprattutto `camera.near = 0.05` con
`far` enorme, che a globo lontano dava un quanto di profondita' di 0,14 unita'
contro una separazione di 0,01. Fermarsi alla deduplica avrebbe lasciato il
difetto quasi intatto, e per giunta il suo indizio migliore - "quando il globo e'
piccolo" - puntava proprio alla causa che non aveva nominato.

**Come applicarlo:** partire dalla sua ipotesi, che e' un buon indizio, ma
misurare fino a spiegare *tutti* i sintomi che ha descritto, compresi gli incisi
che sembrano di contorno ("da telefono", "quando e' piccolo"). Poi dirgli cosa
c'era oltre alla sua diagnosi: lo apprezza, e' la stessa richiesta di
[[utente-vuole-compromessi-espliciti]].

Vedi [[been-there-stato-lavoro]].
