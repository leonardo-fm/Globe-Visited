# Note tecniche

Il perché delle scelte non ovvie. Roba da leggere solo quando qualcosa si rompe
o prima di toccare il rendering. Il README resta corto apposta.

## Il dataset

`countries.geojson` è Natural Earth Admin 0 1:50m semplificato. Per rigenerarlo:

```
curl -O https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson

npx mapshaper ne_50m_admin_0_countries.geojson -filter-fields ISO_A3,ADM0_A3,ISO_A2,NAME,NAME_LONG,NAME_IT,ADMIN -simplify visvalingam 8% keep-shapes -clean -o format=geojson precision=0.01 countries.geojson
```

Perché il 50m e non il 110m: a 1:110m Malta, Singapore e Maldive **non esistono
come poligoni**, quindi non sarebbero né cliccabili né cercabili. Restano fuori
anche dal 50m solo Vaticano, Monaco, San Marino, Liechtenstein, Andorra e alcune
isole del Pacifico, presenti solo a 1:10m.

`keep-shapes` impedisce che le isole minori spariscano nella semplificazione,
`-clean` ripara le auto-intersezioni che la semplificazione introduce,
`precision=0.01` (~1 km) taglia i decimali inutili.

I campi tenuti sono il minimo che serve. **Non aggiungere `ISO_A3_EH` o
`SOV_A3`**: per le dipendenze contengono il codice dello stato sovrano, e usarli
come chiave fa colorare Ashmore e Indian Ocean Ter. insieme all'Australia.

**Identificazione dei paesi.** In Natural Earth alcuni stati hanno
`ISO_A3 = "-99"` (Kosovo, Cipro del Nord, Somaliland, e in certe release anche
Francia e Norvegia). Una funzione unica in `index.html` risolve la chiave con
`ISO_A3 → ADM0_A3 → nome normalizzato`, ed è la stessa che produce i codici
salvati in DataStore. All'avvio la console logga un warning se due feature
collidono sulla stessa chiave.

**Contatore.** Il denominatore è il numero di poligoni del dataset (~242), non
195: Natural Earth include territori non sovrani e non coincide con la lista ONU.

## Verso degli anelli del GeoJSON

All'avvio `index.html` riavvolge in place i poligoni prima di passarli al globo,
e non è un dettaglio cosmetico. three-globe costruisce le calotte con
`three-conic-polygon-geometry`, che si appoggia a **d3-geo**, e d3-geo usa la
convenzione sferica **opposta a RFC 7946**: anello esterno in senso *orario*
(area sferica con segno positiva), buchi in senso antiorario. Il dataset è
RFC 7946, quindi tutti e 470 i poligoni avevano area d3 negativa e d3 li leggeva
come il **complemento** del paese. Da lì:

1. `d3.geoBounds` di un poligono con area negativa restituisce l'intera sfera;
2. la libreria conclude che il poligono contiene i poli e abbandona il ramo di
   triangolazione earcut (che al verso è indifferente) per quello sferico;
3. lì il test punto-dentro-poligono è `geoContains`, che sul complemento è vero
   **fuori** dai confini.

A schermo: la calotta colorata copriva tutto tranne il paese, e il tocco
selezionava un altro paese perché il raycast incontrava la mesh invertita di un
vicino. Il criterio del riavvolgimento (`sphericalRingSum`) è la stessa formula
di `d3-geo/area.js`, così non può divergere da quello che applica la libreria; si
somma su tutti gli anelli del poligono e, se il totale è negativo, si invertono
tutti, preservando il verso relativo fra buchi ed esterno. Il file su disco resta
RFC 7946: rigenerare il GeoJSON non riporta il bug.

## Tassellatura della calotta: i cunei blu dentro i paesi

La calotta di un paese si costruisce triangolando con Delaunay i punti del
contorno più un reticolo di Fibonacci di punti interni, e i triangoli che toccano
il contorno vengono **scartati se il loro baricentro cade fuori dal poligono**.
Vicino a una rientranza del confine capita che un triangolo che copre terra buona
abbia il baricentro appena oltre il bordo: viene buttato, e al suo posto si vede
la sfera. Non sono buchi nel GeoJSON — verificato con una griglia a 3 km su tutta
l'Asia centrale: l'unica zona scoperta è il Caspio.

Col default di globe.gl (`polygonCapCurvatureResolution` = 5 gradi) il pianeta
intero riceve **444 punti interni**: la Russia 54, il Kazakistan una decina. Ma
abbassare la risoluzione *uniformemente* non basta, perché il reticolo è uniforme
e un paese piccolo continua a prendere le briciole: a 1 grado restano 49 paesi
interi con 8 punti o meno — Svizzera 3, Belgio 3, Israele 1, Libano 1, Kuwait 1.

La densità del reticolo è esattamente **1/res² punti per grado quadrato** (i
`(360/res)²/π` punti della sfera divisi per i suoi 41.253 gradi quadrati), quindi
per garantire a un poligono di area A almeno N punti basta `res = sqrt(A/N)`. Da
qui la risoluzione **per paese**: `clamp(sqrt(area/60), 0.25, 0.5)`, calcolata
sul poligono più grande della feature e memorizzata su di essa.

I due limiti sono misurati, non scelti a occhio:

- `MAX = 0.5` tiene il cuneo peggiore sotto i ~55 km anche per la Russia;
- `MIN = 0.25` protegge i micro-stati. Sotto la propria dimensione un poligono
  salta il ramo Delaunay e usa **earcut**, che è esatto e non sbaglia mai:
  scendere sotto 0.25 strappa Malta, Singapore e Andorra da quel ramo sicuro e
  peggiora il totale.

Paesi interi con ≤8 punti interni, al variare della regola: **49** con l'uniforme
a 1 grado, **27** con l'uniforme a 0.5, **15** con questa regola, poi si risale a
24 col minimo a 0.15 e a 45 col minimo a 0.05. Costo: 103.735 test
punto-in-poligono all'avvio contro i 931 del default, una tantum. I 15 che
restano sono territori minuscoli (Guam, Isola di Man, Åland, Fær Øer, Cipro,
Lussemburgo, Brunei).

Attenzione: questo, a differenza dell'altitudine, è un vero parametro della
geometria. L'accessor viene rieseguito a ogni digest, quindi il valore è
memoizzato sulla feature — se cambiasse, ritassellerebbe tutto.

## Ricolore efficiente

Un tocco riesegue solo gli accessor, mai la tassellatura. Nella `update` del
layer poligoni di globe.gl 2.46.2 la geometria si ricostruisce solo se cambiano
coordinate, `polygonCapCurvatureResolution`, `closedTop` o `includeSides` — tutti
costanti qui. Il colore finisce in `material.color`, e **l'altitudine non è un
parametro della geometria**: la calotta nasce come
`ConicPolygonGeometry(coords, 0, RAGGIO, …)` e l'altitudine diventa solo
`group.scale = 1 + alt`. È la ragione per cui il paese selezionato può essere
sollevato senza costo.

## Il popup del paese

Toccare un paese lo seleziona e apre una card ancorata al suo centroide, con
bandiera, nome e un interruttore *Visitato*: selezione e marcatura sono due gesti
distinti, così un tocco distratto sul globo non sporca i dati. Toccare di nuovo
lo stesso paese chiude, toccare l'oceano chiude, scegliere un paese dalla ricerca
o dalla lista dei visitati vola e apre la stessa card.

La card vive nel layer `htmlElements` di globe.gl, cioè in un `CSS2DRenderer`: la
posizione la ricalcola la libreria a ogni frame, quindi resta incollata al paese
mentre la sfera ruota senza una riga di codice di posizionamento e senza
attraversare il ponte JS↔Kotlin a 60 fps. È anche il motivo per cui il popup sta
nella WebView e non in Compose. `htmlElementVisibilityModifier` dice quando il
punto è passato dietro l'orizzonte, e la card svanisce in dissolvenza restando
selezionata.

**Il tocco sulla card non deve far partire il raycast**, e `pointer-events: auto`
da solo non basta: globe.gl ascolta il `pointerup` sul contenitore della scena e
lo fa **in fase di cattura**, mentre il layer CSS2D — quindi la card — vive
dentro quel contenitore. Un listener in cattura su un antenato scatta prima del
bersaglio, perciò nessuno `stopPropagation` lanciato dalla card arriva in tempo.
Senza contromisure premere l'interruttore selezionava anche il paese sotto la
card, e se sotto c'era oceano il popup si chiudeva da solo. La soluzione non è
intercettare l'evento ma scartarlo: le callback ricevono l'evento DOM originale
(`onPolygonClick(poligono, evento, coords)`, `onGlobeClick(coords, evento)`), e
`fromPopup()` guarda `event.target`. Stelo e pallino restano a
`pointer-events: none`, così lasciano passare il tocco al paese.

**Il contorno del selezionato si accende di bianco** (`BORDER_SEL`), scelto
perché non è né il grigio del non visitato né l'arancione del visitato. Lo
spessore non è regolabile: il contorno è un `LineSegments` con
`LineBasicMaterial`, e `linewidth` in WebGL viene ignorato.

**Il paese selezionato viene sollevato** (`SEL_LIFT`), e non è un vezzo: senza,
solo alcuni segmenti del contorno si illuminavano. Ogni paese è una fetta estrusa
alta `POLY_ALT`, cioè 0,6 unità su un globo di raggio 100, e three-globe mette il
contorno appena 1e-4 di scala sopra la calotta, 0,01 unità. Ma i confini sono
condivisi: lungo una frontiera il contorno di A e quello di B sono geometrie
**coincidenti**, e i muri laterali dei vicini — invisibili, perché
`polygonSideColor` è trasparente, ma che scrivono comunque nel depth buffer —
arrivano alla stessa quota. Il bianco vinceva il test di profondità a macchia di
leopardo. Alzando il selezionato di 0,2 unità, 20 volte quel margine, il conflitto
sparisce invece di essere solo reso meno probabile.

## Bandiere

Sono emoji costruite dall'`ISO_A2` con due indicatori regionali (`IT` → 🇮🇹).
Natural Earth mette `ISO_A2 = "-99"` su 9 feature; per Francia, Norvegia, Kosovo
e Taiwan il codice si recupera da `ADM0_A3` con una tabella esplicita, e restano
senza bandiera solo le 5 entità che un codice ISO non ce l'hanno (Somaliland,
Cipro del Nord, Siachen, Ashmore e Cartier, Indian Ocean Ter.). Copertura: 237
paesi su 242.

Le emoji di sistema però non bastano: **Windows non ha i glifi delle bandiere**,
Microsoft li ha esclusi apposta da Segoe UI Emoji, e Chrome su Windows disegna le
due lettere separate — `🇮🇹` diventa `IT`. Per questo negli asset c'è
`TwemojiCountryFlags.woff2`, che contiene **solo** le bandiere: dichiarato in
`@font-face` e messo per primo nel `font-family` di `.flag`, le risolve ovunque.
Lo `unicode-range: U+1F1E6-1F1FF` lo limita agli indicatori regionali, quindi non
viene caricato finché non si apre un popup con bandiera.

Nota per il futuro: `WebViewAssetLoader` può servire il `.woff2` con un MIME type
generico, perché su Android 8 quell'estensione non è nella tabella di sistema.
Non è un problema — i font non sono soggetti al controllo del MIME type — ma è il
primo posto dove guardare se un giorno le bandiere sparissero.

## Il Caspio è un buco, e non è un difetto

A ovest del Kazakistan si vede una grossa macchia del colore dell'oceano: è il
Mar Caspio, che Natural Earth esclude dai poligoni admin-0 trattandolo come mare,
insieme al golfo di Kara-Bogaz-Gol. Ogni punto non coperto da un paese lascia
vedere la sfera, che è blu.

Non c'entrano né la semplificazione né fessure fra confini: sul `ne_50m`
originale non semplificato i punti scoperti della regione sono gli stessi (691
contro 683, differenza tutta sui bordi), e una griglia a 3 km su tutta l'Asia
centrale trova una sola zona scoperta. La stessa convenzione produce
un'incoerenza che salta all'occhio: il Lago d'Aral e il Balqaš stanno *dentro* i
poligoni dei paesi e si colorano come terra, il Caspio no. Riempirlo vorrebbe
dire tracciare linee mediane fra i cinque stati rivieraschi, cioè prendere
posizione su una spartizione che ha richiesto una convenzione internazionale nel
2018.

## Dettagli minori

**`[hidden]` ha bisogno di `!important`.** La regola `[hidden] { display: none }`
è del foglio di stile del browser, e qualsiasi regola d'autore la batte:
`#ui { display: flex }` vinceva, quindi su Android la chrome HTML restava a
schermo sopra la UI Compose, raddoppiando contatore e campo di ricerca.

**Ricerca.** Cerca sul nome nella lingua selezionata più il codice ISO, senza
distinguere accenti. Per gli stati insulari troppo piccoli da colpire con il
dito, il pallino a destra di ogni risultato è la via di selezione.

**Lingua.** Selettore manuale IT/EN nella bottom sheet, salvato in DataStore. Non
passa dal locale di sistema perché `LocaleManager` è API 33+ e qui minSdk è 26:
le traduzioni stanno in `res/values` e `res/values-en`, lette da `appString()`
con un `Resources` a locale sovrascritto. La lingua deve arrivare anche dentro la
WebView, che disegna da sé nome e pulsante del popup:
`GlobeCommand.SetLanguage` la spinge con `BeenThere.setLanguage()`. Le stringhe
della pagina stanno nell'oggetto `STR` di `index.html`.

**Rotazione.** L'activity dichiara `configChanges`, così ruotando il telefono la
WebView non viene distrutta e il globo non si ricarica.
