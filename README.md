# Been There

Mappa personale dei paesi visitati, con globo 3D interattivo. Tutto offline:
l'app **non dichiara il permesso INTERNET** e non fa una sola richiesta di rete.

- Guscio nativo Kotlin + Jetpack Compose: contatore, ricerca, lista dei visitati.
- Globo reso in una WebView a schermo intero con [globe.gl](https://globe.gl) (three.js).
- Persistenza in DataStore Preferences. La verità sta lato Kotlin, non nella WebView.

## Gli asset del globo

`app/src/main/assets/` contiene gia' i tre file necessari, versionati nel repo
perche' senza di loro l'app non funziona e non c'e' rete da cui prenderli a
runtime:

- `globe.gl.min.js` - build UMD standalone di globe.gl **2.46.2** (three incluso),
  1,8 MB, presa da `https://cdn.jsdelivr.net/npm/globe.gl@2.46.2/dist/globe.gl.min.js`
- `countries.geojson` - Natural Earth Admin 0 1:50m semplificato, 242 paesi, 193 KB
- `TwemojiCountryFlags.woff2` - font con i soli glifi delle bandiere, 78 KB,
  pacchetto MIT, glifi Twemoji (CC-BY 4.0). Vedi *Bandiere* piu' sotto.
  ```
  curl -o TwemojiCountryFlags.woff2 https://cdn.jsdelivr.net/npm/country-flag-emoji-polyfill@0.1.10/dist/TwemojiCountryFlags.woff2
  ```

Per rigenerare il GeoJSON da capo:

```
curl -O https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson

npx mapshaper ne_50m_admin_0_countries.geojson -filter-fields ISO_A3,ADM0_A3,ISO_A2,NAME,NAME_LONG,NAME_IT,ADMIN -simplify visvalingam 8% keep-shapes -clean -o format=geojson precision=0.01 countries.geojson
```

Perche' il 50m e non il 110m della prima bozza: a 1:110m Malta, Singapore e
Maldive **non esistono come poligoni**, quindi non sarebbero ne' cliccabili ne'
cercabili. Restano fuori anche dal 50m soltanto Vaticano, Monaco, San Marino,
Liechtenstein, Andorra e alcune isole del Pacifico, presenti solo a 1:10m.

`keep-shapes` impedisce che le isole minori spariscano nella semplificazione,
`-clean` ripara le auto-intersezioni che la semplificazione introduce,
`precision=0.01` (~1 km) taglia i decimali inutili.

I campi tenuti sono il minimo che servono al globo e alla ricerca. **Non
aggiungere `ISO_A3_EH` o `SOV_A3`**: per le dipendenze contengono il codice
dello stato sovrano, e usarli come chiave fa colorare Ashmore e Indian Ocean
Ter. insieme all'Australia.

## Compilazione

```
./gradlew assembleDebug
```

L'APK esce in `app/build/outputs/apk/debug/app-debug.apk`.

Requisiti: JDK 17+ (testato con 21), compileSdk 35, minSdk 26. Il file
`local.properties` con `sdk.dir` non e' versionato: lo crea Android Studio, o lo
scrivi a mano con gli slash in avanti (`sdk.dir=C:/percorso/Android/Sdk` -
attenzione, in un file .properties i backslash singoli sono escape e il percorso
verrebbe letto storto).

## Provare il globo nel browser, senza Android

`app/src/main/assets/index.html` è autonomo: se `AndroidBridge` non esiste
accende da solo la propria interfaccia HTML e salva in `localStorage`. Non
aprirlo con doppio clic — da `file://` il browser blocca la lettura del GeoJSON.

```
cd app/src/main/assets
python -m http.server 8000
```

e apri `http://localhost:8000/index.html`.

## Come sono organizzate le cose

```
app/src/main/assets/index.html          il globo: rendering, selezione, popup, colori
app/src/main/java/com/beenthere/app/
  MainActivity.kt                       edge-to-edge, tema
  MainViewModel.kt                      stato unico + comandi verso il globo
  data/Country.kt                       modello, catalogo, ricerca
  data/SettingsRepository.kt            DataStore: visitati + lingua
  globe/GlobeBridge.kt                  JS -> Kotlin (@JavascriptInterface)
  globe/GlobeWebView.kt                 WebView + WebViewAssetLoader, Kotlin -> JS
  ui/                                   Compose: contatore, ricerca, bottom sheet
```

**Il flusso dei dati.** A globo pronto il JavaScript passa a Kotlin l'intero
catalogo dei paesi in una sola chiamata; da lì la ricerca è tutta nativa. Un
tocco sul globo **non segna niente**: apre il popup del paese, e solo
l'interruttore dentro il popup marca il visitato, colorando subito lato JS e
facendolo persistere da Kotlin. I toggle nati dalla UI nativa vengono persistiti
e poi spinti al globo con `evaluateJavascript`. Non c'è mai un giro di ritorno
che ridisegni due volte.

## Punti che è utile conoscere

**Popup del paese.** Toccare un paese lo seleziona e apre una card ancorata al
suo centroide, con bandiera, nome e un interruttore *Visitato*: la selezione e
la marcatura sono due gesti distinti, e un tocco distratto sul globo non sporca
più i dati. Toccare di nuovo lo stesso paese chiude, toccare l'oceano chiude,
scegliere un paese dalla ricerca o dalla lista dei visitati vola e apre la
stessa card.

Il contorno del paese selezionato si accende di bianco (`BORDER_SEL`): è
l'aggancio visivo fra la card e la sagoma, e conta soprattutto per i paesi
piccoli, dove la card copre quasi tutto quello che indica. Il bianco è scelto
perché non è né il grigio del non visitato né l'arancione del visitato, quindi
"selezionato" resta leggibile sopra entrambi. Lo spessore non è regolabile — il
contorno è un `LineSegments` con `LineBasicMaterial`, e `linewidth` in WebGL
viene ignorato — quindi il segnale è solo cromatico.

**Il paese selezionato viene sollevato** (`SEL_LIFT`), e non è un vezzo: senza,
solo alcuni segmenti del contorno si illuminavano. Ogni paese è una fetta estrusa
alta `POLY_ALT`, cioè 0,6 unità su un globo di raggio 100, e three-globe mette il
contorno appena 1e-4 di scala sopra la calotta, 0,01 unità. Ma i confini sono
condivisi: lungo una frontiera il contorno di A e quello di B sono geometrie
**coincidenti**, e i muri laterali dei vicini — invisibili, perché
`polygonSideColor` è trasparente, ma che scrivono comunque nel depth buffer —
arrivano esattamente alla stessa quota. Il bianco vinceva il test di profondità
a macchia di leopardo. Alzando il selezionato di 0,2 unità, 20 volte quel
margine, il suo contorno passa sopra muri e contorni di tutti i vicini: il
conflitto sparisce invece di essere solo reso meno probabile. Il sollevamento è
poco visibile perché le pareti laterali sono trasparenti.

La card vive nel layer `htmlElements` di globe.gl, cioè in un `CSS2DRenderer`:
la posizione la ricalcola la libreria a ogni frame, quindi resta incollata al
paese mentre la sfera ruota senza una riga di codice di posizionamento e senza
attraversare il ponte JS↔Kotlin a 60 fps. È anche il motivo per cui il popup sta
nella WebView e non in Compose. Il contenitore di quel layer ha
`pointer-events: none`, così il trascinamento del globo ci passa sotto: solo la
card se li riaccende per sé. `htmlElementVisibilityModifier` dice quando il
punto è passato dietro l'orizzonte del globo, e la card svanisce in dissolvenza
restando selezionata.

**Il tocco sulla card non deve far partire il raycast**, e `pointer-events: auto`
da solo non basta: globe.gl ascolta il `pointerup` sul contenitore della scena e
lo fa **in fase di cattura**, mentre il layer CSS2D — quindi la card — vive
dentro quel contenitore. Un listener in cattura su un antenato scatta prima del
bersaglio, perciò nessuno `stopPropagation` lanciato dalla card arriva in tempo.
Senza contromisure premere l'interruttore selezionava anche il paese sotto la
card, e se sotto c'era oceano il popup si chiudeva da solo. La soluzione non è
intercettare l'evento ma scartarlo: le callback di globe.gl ricevono l'evento
DOM originale (`onPolygonClick(poligono, evento, coords)`,
`onGlobeClick(coords, evento)`), e `fromPopup()` guarda `event.target` per
sapere dove il dito è atterrato davvero. Stelo e pallino restano a
`pointer-events: none`, così continuano a lasciar passare il tocco al paese.

**Bandiere.** Sono emoji costruite dall'`ISO_A2` con due indicatori regionali
(`IT` → 🇮🇹): zero asset, zero rete, le disegna il font di sistema. Natural Earth
mette `ISO_A2 = "-99"` su 9 feature; per Francia, Norvegia, Kosovo e Taiwan il
codice si recupera da `ADM0_A3` con una tabella esplicita, e restano senza
bandiera solo le 5 entità che un codice ISO non ce l'hanno (Somaliland, Cipro
del Nord, Siachen, Ashmore e Cartier, Indian Ocean Ter.), che mostrano il solo
nome. Copertura: 237 paesi su 242.

Le emoji di sistema però non bastano: **Windows non ha i glifi delle bandiere**,
Microsoft li ha esclusi apposta da Segoe UI Emoji, e Chrome su Windows non
trovando la coppia di indicatori regionali disegna le due lettere separate —
`🇮🇹` diventa `IT`. Per questo negli asset c'è `TwemojiCountryFlags.woff2`, un
font che contiene **solo** le bandiere: dichiarato in `@font-face` e messo per
primo nel `font-family` di `.flag`, le risolve su ogni piattaforma. Lo
`unicode-range: U+1F1E6-1F1FF` lo limita agli indicatori regionali, quindi non
viene nemmeno caricato finché non si apre un popup con bandiera.

**Identificazione dei paesi.** In Natural Earth alcuni stati hanno
`ISO_A3 = "-99"` (Kosovo, Cipro del Nord, Somaliland, e in certe release anche
Francia e Norvegia). Una funzione unica in `index.html` risolve la chiave con
`ISO_A3 → ADM0_A3 → nome normalizzato`, ed è la stessa che
produce i codici salvati in DataStore. All'avvio la console logga un warning se
due feature collidono sulla stessa chiave.

**Verso degli anelli del GeoJSON.** All'avvio `index.html` riavvolge in place i
poligoni prima di passarli al globo, e non e' un dettaglio cosmetico. three-globe
costruisce le calotte con `three-conic-polygon-geometry`, che si appoggia a
**d3-geo**, e d3-geo usa la convenzione sferica **opposta a RFC 7946**: anello
esterno in senso *orario* (area sferica con segno positiva), buchi in senso
antiorario. Il nostro dataset e' RFC 7946, quindi tutti e 470 i poligoni avevano
area d3 negativa e d3 li leggeva come il **complemento** del paese. Da li':

1. `d3.geoBounds` di un poligono con area negativa restituisce l'intera sfera;
2. la libreria conclude che il poligono contiene i poli e abbandona il ramo di
   triangolazione earcut (che al verso e' indifferente) per quello sferico;
3. li' il test punto-dentro-poligono e' `geoContains`, che sul complemento e'
   vero **fuori** dai confini.

A schermo: la calotta colorata copriva tutto tranne il paese, e il tocco
selezionava un altro paese perche' il raycast incontrava la mesh invertita di un
vicino. Il criterio del riavvolgimento (`sphericalRingSum`) e' la stessa formula
di `d3-geo/area.js`, cosi' non puo' divergere da quello che applica la libreria;
si somma su tutti gli anelli del poligono e, se il totale e' negativo, si
invertono tutti, preservando il verso relativo fra buchi ed esterno. Il file su
disco resta RFC 7946: rigenerare il GeoJSON non riporta il bug.

**Ricolore efficiente.** Un tocco riesegue solo gli accessor, mai la
tassellatura. Nella `update` del layer poligoni di globe.gl 2.46.2 la geometria
si ricostruisce solo se cambiano coordinate, `polygonCapCurvatureResolution`,
`closedTop` o `includeSides` — tutti costanti qui. Il colore finisce in
`material.color`, e **l'altitudine non è un parametro della geometria**: la
calotta nasce come `ConicPolygonGeometry(coords, 0, RAGGIO, …)` e l'altitudine
diventa solo `group.scale = 1 + alt`. Il README diceva il contrario, che
cambiare altitudine rigenerasse la geometria: valeva per una versione
precedente, ed è la ragione per cui il paese selezionato ora può essere
sollevato senza costo.

**Tassellatura della calotta: i cunei blu dentro i paesi.** La calotta di un
paese si costruisce triangolando con Delaunay i punti del contorno più un
reticolo di Fibonacci di punti interni, e i triangoli che toccano il contorno
vengono **scartati se il loro baricentro cade fuori dal poligono**. Vicino a una
rientranza del confine capita che un triangolo che copre terra buona abbia il
baricentro appena oltre il bordo: viene buttato, e al suo posto si vede la
sfera. Non sono buchi nel GeoJSON — verificato con una griglia a 3 km su tutta
l'Asia centrale: l'unica zona scoperta è il Caspio.

Col default di globe.gl (`polygonCapCurvatureResolution` = 5 gradi) il pianeta
intero riceve **444 punti interni**: la Russia 54, il Kazakistan una decina. Ma
abbassare la risoluzione *uniformemente* non basta, perché il reticolo è
uniforme e un paese piccolo continua a prendere le briciole: a 1 grado restano
49 paesi interi con 8 punti o meno — Svizzera 3, Belgio 3, Israele 1, Libano 1,
Kuwait 1.

La densità del reticolo è esattamente **1/res² punti per grado quadrato** (i
`(360/res)²/π` punti della sfera divisi per i suoi 41.253 gradi quadrati),
quindi per garantire a un poligono di area A almeno N punti basta
`res = sqrt(A/N)`. Da qui la risoluzione **per paese**:
`clamp(sqrt(area/60), 0.25, 0.5)`, calcolata sul poligono più grande della
feature e memorizzata su di essa.

I due limiti sono misurati, non scelti a occhio:

- `MAX = 0.5` tiene il cuneo peggiore sotto i ~55 km anche per la Russia;
- `MIN = 0.25` protegge i micro-stati. Sotto la propria dimensione un poligono
  salta il ramo Delaunay e usa **earcut**, che è esatto e non sbaglia mai:
  scendere sotto 0.25 strappa Malta, Singapore e Andorra da quel ramo sicuro e
  peggiora il totale.

Paesi interi con ≤8 punti interni, al variare della regola: **49** con
l'uniforme a 1 grado, **27** con l'uniforme a 0.5, **15** con questa regola,
poi si risale a 24 col minimo a 0.15 e a 45 col minimo a 0.05. Costo: 103.735
test punto-in-poligono all'avvio contro i 931 del default, una tantum. I 15 che
restano sono territori minuscoli (Guam, Isola di Man, Åland, Fær Øer, Cipro,
Lussemburgo, Brunei).

Attenzione: questo, a differenza dell'altitudine, è un vero parametro della
geometria. L'accessor viene rieseguito a ogni digest, quindi il valore è
memoizzato sulla feature — se cambiasse, ritassellerebbe tutto.

**Il Caspio è un buco, e non è un difetto.** A ovest del Kazakistan si vede una
grossa macchia del colore dell'oceano: è il Mar Caspio, che Natural Earth
esclude dai poligoni admin-0 trattandolo come mare, insieme al suo golfo di
Kara-Bogaz-Gol. Ogni punto non coperto da un paese lascia vedere la sfera, che è
blu. Verificato che non c'entrano né la semplificazione né le fessure fra
confini: sul `ne_50m` originale non semplificato i punti scoperti della regione
sono gli stessi (691 contro 683, differenza tutta sui bordi), e una griglia a
0,05° sull'intero confine Kazakistan-Uzbekistan non trova **nessun** punto
scoperto. La stessa convenzione produce un'incoerenza che salta all'occhio: il
Lago d'Aral e il Balqaš stanno *dentro* i poligoni dei paesi e quindi si colorano
come terra, il Caspio no. È una scelta di Natural Earth, non nostra; riempirlo
vorrebbe dire tracciare linee mediane fra i cinque stati rivieraschi, cioè
prendere posizione su una spartizione che ha richiesto una convenzione
internazionale nel 2018.

**Contatore.** Il denominatore è il numero di poligoni presenti nel dataset
(~242), non 195: Natural Earth include territori non sovrani e non coincide con
la lista ONU. Ogni feature è cliccabile e contata singolarmente.

**Ricerca.** Cerca sul nome nella lingua selezionata più il codice ISO, senza
distinguere accenti. Per gli stati insulari troppo piccoli da colpire con il
dito, il pallino a destra di ogni risultato è la via di selezione.

**Lingua.** Selettore manuale IT/EN nella bottom sheet, salvato in DataStore.
Non passa dal locale di sistema perché `LocaleManager` è API 33+ e qui minSdk è
26: le traduzioni restano in `res/values` e `res/values-en`, lette da
`appString()` con un `Resources` a locale sovrascritto. Da quando esiste il
popup la lingua deve arrivare anche dentro la WebView, che disegna da sé nome e
pulsante: `GlobeCommand.SetLanguage` la spinge con `BeenThere.setLanguage()`,
all'avvio e a ogni cambio. Le poche stringhe della pagina stanno nell'oggetto
`STR` di `index.html`; i nomi dei paesi passano da `NAME_IT` a `NAME`.

**Rotazione.** L'activity dichiara `configChanges`, così ruotando il telefono la
WebView non viene distrutta e il globo non si ricarica.
