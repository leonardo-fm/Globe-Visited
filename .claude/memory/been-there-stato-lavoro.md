---
name: been-there-stato-lavoro
description: "Punto in cui si e' fermato il lavoro su Been There al 2026-09-09: schermata impostazioni, colore scelto dall'utente e icona nuova, tutto provato sul telefono"
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

**2026-09-07: cominciata la funzione dei luoghi** (citta' di catalogo + pin
personali), concordata a fasi - vedi [[lavorare-a-fasi-verificabili]] e il piano
in C:/Users/Lo/.claude/plans/quello-che-voglio-io-happy-turtle.md. Fase 1
consegnata e confermata dall'utente nel browser: cities.json negli asset (4.205
citta') e la ricerca che le trova. Fase 2 consegnata e da provare: i luoghi si
accendono, sono un solo oggetto in scena (5 draw call invece di 4) e il pin si
tocca proiettandolo a schermo, non col raycast. Fase 3 consegnata: pressione
lunga sul globo e creazione da coordinate incollate. **Su richiesta dell'utente
la fase 3 e' stata scritta senza provarla** (solo controlli statici e uno
screenshot) e **senza la modifica dei pin**, rimandata: un pin storto si
cancella e si rifa. Paesi e luoghi sono indipendenti e il contatore resta
solo sui paesi.

**2026-09-07, secondo piano (correzioni dopo il collaudo dei luoghi):** tutti e
quattro i punti risultano **scritti nel working tree ma non committati e non
provati sul telefono**. (1) La ricerca si azzera sul cambio di fuoco in
`ui/SearchPanel.kt`, con il ripiego gia' dentro: azzera sia quando perde il fuoco
sia quando lo riacquista. (2) I pin sono sfere costruite a mano
(`buildPlaceMesh()` in `index.html`), non piu' il layer punti di globe.gl.
(3) Le quote dei gusci passano da una costante sola `SHELL_GAP = 0.001` (era
0.003). (4) Il backup JSON c'e' tutto: `data/Backup.kt` (file nuovo, non
tracciato), export/import nel `MainViewModel`, i due launcher SAF e il dialogo di
conferma in `BeenThereScreen.kt`, le due voci nella bottom sheet, le stringhe in
entrambe le lingue, e `docs/note-tecniche.md` aggiornato.

Il JS di `index.html` passa il controllo sintattico. Quello che manca e' solo la
prova sull'S10e - in particolare il fuoco della ricerca, lo sfarfallio a globo
piccolo con le quote abbassate, e il giro esporta -> disinstalla -> reinstalla ->
importa - e il commit.

**2026-09-08, due difetti trovati usando l'app**, corretti in
`ui/SearchPanel.kt` e ancora da provare sul telefono. (1) Nella riga di una
citta' il nome e il paese sembravano attaccati: le due righe erano impilate
senza spazio, ora c'e' `Arrangement.spacedBy(3.dp)` e `lineHeight` fissata su
entrambe. (2) La ricerca continuava a non azzerarsi, ed e' il punto che conta:
azzerarla **sul fuoco non puo' funzionare**, perche' toccando il globo il fuoco
lo prende la WebView senza che Compose se ne accorga - per Compose il campo non
lo perde mai, quindi ritoccandolo non c'e' nessun cambio da notificare e
`onFocusChanged` non parte. Ora si guarda il **tocco** (`pointerInput` +
`awaitFirstDown(requireUnconsumed = false)` sul contenitore della barra), che
arriva sempre. Prezzo detto all'utente: toccare il campo a meta' parola
riazzera anche mentre si scrive.

Da qui e' nato [[banco-di-prova-web]], per provare i flussi nel browser prima
di passare al telefono.

**2026-09-08, la ricerca cambia regola.** L'azzeramento automatico e' stato
**tolto su richiesta dell'utente**: era appena stato fatto funzionare, ma nell'uso
dava fastidio. Ora la lista dei risultati ha uno stato suo, `resultsOpen`,
separato dal testo: toccando il globo la lista si chiude e la parola resta
scritta, si torna sulla barra e la si corregge o si cancella con la X. Toccare
la barra riapre la lista. Il segnale del tocco sul globo e' un `pointerInput`
nella passata **Initial** attorno alla WebView, in `BeenThereScreen.kt`, che non
consuma il gesto: il fuoco resta inaffidabile per il motivo gia' scritto sopra.

**2026-09-08, schermata delle impostazioni** (`ui/SettingsScreen.kt`, file
nuovo), aperta da un ingranaggio accanto al contatore. Lingua e backup sono
usciti dal foglio dei visitati, che torna a fare solo l'elenco. Dentro, oltre a
quelli: **colore dei visitati** (sei pastiglie in `data/VisitedColors.kt` piu' un
campo dove scrivere un esadecimale qualsiasi), **mostra/nascondi i pin**,
**azzera tutti i dati** con conferma coi numeri, e **informazioni** con versione
e crediti (Natural Earth, globe.gl/three.js, Twemoji).

Tre cose non ovvie di quel giro: il colore arriva alla UI nativa da un
`LocalVisitedColor` nel tema e al globo da `BeenThere.setVisitedColor(hex)`, che
ridipinge le calotte **e** aggiorna la variabile CSS `--visited`; ogni
`SetPlaces` verso il globo passa da `emitPlaces()` nel `MainViewModel`, unico
punto che rispetta i pin nascosti (erano cinque punti sparsi); e "azzera" non
tocca lingua, colore e preferenze, perche' ricominciare da capo non e'
reinstallare l'app.

**2026-09-08/09, icona rifatta.** Non piu' una sfera disegnata a mano ma la
Terra vera: `tools/build-icon.mjs` proietta i confini di `countries.geojson` in
ortografica e genera i vector drawable - vedi [[icona-generata-dai-confini]].

**Confermato dall'utente sul suo S10e il 2026-09-09: "funziona tutto".** Con
questo cadono tutte le prove rimaste in sospeso qui sopra (fuoco della ricerca,
sfarfallio a globo piccolo, giro completo del backup, tempo di avvio col dataset
grande, fase 3 dei luoghi). Tutto committato da lui: `added options`,
`added custom color`, `fixed build for phone`, `improoved icon`.

**2026-09-09, zoom piu' ravvicinato.** `MIN_ALT` (nuova costante in
`index.html`, con `MAX_ALT`) passa da 0,30 a **0,12**: i limiti dei controlli
non si scrivono piu' in unita' di mondo ma in altitudine, come tutto il resto
del file. Chiesto dall'utente perche' piantare un pin con la pressione lunga
vuol dire centrare un punto col polpastrello. Superficie 2,5 volte piu' grande.
**Confermato da lui: "lo zoom ha funzionato".** Il ragionamento e i due effetti
collaterali (near dei piani di taglio che tocca il suo minimo, camera dentro la
sfera dell'atmosfera) stanno in `docs/note-tecniche.md`.

**2026-09-09, il crash "quando esporto".** Non era l'export: era
`LocalAppResources non fornito`. In `BeenThereScreen.kt` il testo del messaggio
di esito veniva risolto con `appString` **sopra** `ProvideAppLanguage`, dove il
CompositionLocal non ha valore e il default e' un `error()`. Con `notice` a null
il ramo non si percorreva mai, quindi il difetto e' rimasto invisibile fino al
primo esito. Non riguardava solo l'export ma **anche import e azzeramento**:
tutti e cinque i `BackupNotice`. Corretto spostando `noticeText` e la
`LaunchedEffect` dentro il provider. Lezione da ricordare: un CompositionLocal
letto in un ramo condizionale non si manifesta al primo avvio, si manifesta il
giorno in cui quel ramo viene percorso.

**Da questa macchina ora si compila** (`./gradlew assembleDebug`): la nota che
diceva il contrario e' stata corretta, vedi
[[ambiente-senza-toolchain-android]]. L'app pero' continua a farla girare
l'utente: chiede esplicitamente di non avviarla da qui.

Vedi [[been-there-decisioni-prodotto]].
