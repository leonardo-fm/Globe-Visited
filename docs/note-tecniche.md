# Note tecniche

Il perché delle scelte non ovvie. Roba da leggere solo quando qualcosa si rompe
o prima di toccare il rendering. Il README resta corto apposta.

## Il dataset

`countries.geojson` è Natural Earth Admin 0 1:50m semplificato. Per rigenerarlo:

```
curl -O https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson

npx mapshaper ne_50m_admin_0_countries.geojson -filter-fields ISO_A3,ADM0_A3,ISO_A2,NAME,NAME_LONG,NAME_IT,ADMIN -simplify visvalingam 40% keep-shapes -clean -o format=geojson precision=0.001 countries.geojson
```

Ne escono **242 feature, 1359 poligoni, 41.128 vertici, 719 KB**.

Perché il 50m e non il 110m: a 1:110m Malta, Singapore e Maldive **non esistono
come poligoni**, quindi non sarebbero né cliccabili né cercabili.

**I microstati ci sono.** Vaticano, Monaco, San Marino, Liechtenstein e Andorra
sono nel file, ed è merito di `keep-shapes`: senza, la semplificazione li
cancellerebbe. Ma sono poligoni di **quattro punti larghi pochi chilometri**, il
Vaticano circa 1×1 km, quindi sul globo valgono una manciata di pixel. Esistono,
si cercano e si segnano dalla lista — col dito sul globo non si prendono. Non è
un difetto del dataset: è il limite fisico di un bersaglio grande un pixel, e la
via d'uscita sarebbe una tolleranza al tocco, non più risoluzione. (Una versione
precedente di questa nota diceva che erano assenti dal 50m: era falso.)

`keep-shapes` impedisce che le isole minori spariscano nella semplificazione,
`-clean` ripara le auto-intersezioni che la semplificazione introduce.

**Perché 40% e non 8%.** Fino al 2026-09-06 la semplificazione era all'8% con
`precision=0.01`, e il risultato pesava 189 KB con 9.708 vertici. Quei due valori
insieme buttavano via molto più di quanto sembrasse: il 50m grezzo ha **99.613
vertici in 1.620 poligoni**, e la vecchia pipeline ne consegnava 470. I 1.150
poligoni mancanti erano isole: `precision=0.01` vale ~1,1 km, e qualunque isola
più piccola collassa in un anello degenere che poi sparisce. Con i parametri
attuali i poligoni sono 1.359 — tornano arcipelaghi interi, per esempio le Hawaii
passano da 2 a 7 isole, e Oahu esiste di nuovo.

Il costo è tutto **all'avvio**, non per frame: la tassellatura è passata da ~0,85
a ~2,0 secondi sul PC, mentre i triangoli sono cresciuti solo del 17% e le draw
call sono rimaste quattro. È il motivo per cui esiste la schermata di
caricamento.

**Attenzione se si tocca `precision`.** La deduplica dei confini si aggancia
all'uguaglianza *esatta* dei vertici condivisi (vedi *Un confine solo per
frontiera*). Mapshaper preserva la topologia e applica lo snap in modo coerente
sugli archi condivisi, quindi finora ha sempre retto — ma è una proprietà da
**riverificare a ogni rigenerazione**, perché se si rompesse i confini
tornerebbero a sfarfallare e il sintomo sembrerebbe scollegato dal dataset. Il
controllo è: contare quante volte compare ogni spigolo, e pretendere che nessuno
compaia più di due volte. Sui parametri attuali: 39.763 spigoli, 32.440 unici,
**7.323 condivisi, zero con tre o più occorrenze**.

Nota sui punti di prova: alzando la risoluzione la costa risolve i fiordi, quindi
una **città sul mare** può legittimamente finire fuori dal poligono del suo paese
(Nuuk passa da 7,7 a 2,0 km dal bordo, e ne esce). Per collaudare il tocco vanno
usati punti **interni**, non capoluoghi costieri.

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

## Il catalogo delle città

`cities.json` è Natural Earth Admin 0 1:10m *populated places*, filtrato e
ridotto da `tools/build-cities.mjs`. Per rigenerarlo:

```
curl -O https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_10m_populated_places.geojson

node tools/build-cities.mjs ne_10m_populated_places.geojson app/src/main/assets/cities.json
```

Il filtro tiene **capitali di stato, capoluoghi di regione e tutto quello che
supera i 100.000 abitanti**: ne escono **4.205 città sulle 7.342 del dataset,
295 KB**, distribuite su 223 codici paese.

Tre scelte che riducono il file senza perdere niente:

- **il nome italiano si scrive solo dove differisce dall'inglese.** Succede 735
  volte su 4.205 (Firenze, Venezia, Città del Vaticano); nelle altre ripeterlo
  raddoppierebbe il campo per niente. Chi legge ricade sul nome inglese, la
  stessa regola che vale per `NAME_IT` nei paesi;
- **coordinate a tre decimali**, cioè ~110 m: il pin è un puntino su un globo, e
  anche a zoom massimo un pixel vale ~900 m (era ~2 km prima che `MIN_ALT`
  scendesse a 0,12), cioè otto volte i 110 m;
- **l'ordine del file è per popolazione decrescente**, e la ricerca lo conserva
  dentro ogni gruppo di match invece di riordinare alfabeticamente. È il motivo
  per cui cercando "new york" esce prima New York City e non New York Mills.

L'id è `<ADM0_A3>:<nome ascii>` — `USA:new-york` — con un contatore in coda per
le 29 omonimie interne allo stesso paese. È **stabile fra rigenerazioni**: un
luogo salvato in DataStore non si rompe se un giorno il file si rigenera.

**Il file lo legge Kotlin, non la WebView.** È l'opposto di quello che succede
per i paesi, ed è deliberato: il globo non ha bisogno del catalogo (riceve solo
i luoghi che l'utente ha piantato, poche decine), mentre la ricerca è tutta
nativa. Farlo passare dal ponte JS→Kotlin significherebbe leggerlo e
serializzarlo due volte per far arrivare 300 KB dove possono arrivare da soli.

Resta però un problema di identità: la città porta l'`ADM0_A3` di Natural Earth,
non la chiave con cui l'app identifica i paesi (`ISO_A3 → ADM0_A3 → nome
normalizzato`, vedi *Identificazione dei paesi*). L'aggancio si fa **dentro il
catalogo dei paesi**, che ora porta anche `codes` — l'ISO_A3 e l'ADM0_A3 della
feature — e `flag`. Così il ponte resta uno solo e la chiave continua a nascere
in un posto solo. Verificato sul 50m: **nessun codice è conteso** fra due paesi,
e solo **3 città su 4.205** (Gibilterra, Svalbard, Tokelau) portano un codice che
non corrisponde a nessuna feature: restano cercabili, senza bandiera.

Non si è usato `countryAt()` per dedurre il paese dalle coordinate, che pure
esiste già ed è esatto: una città sul mare può cadere **fuori** dal poligono del
suo paese quando la costa è semplificata — è la stessa trappola di Nuuk descritta
sopra — mentre il codice del dataset non ha quel problema.

## I luoghi: un pin, una draw call

Un luogo è una perlina bianca sul globo, e le perline sono **un oggetto solo**:
tutte le sferette fuse in una `BufferGeometry`. Il pianeta passa da 4 a **5 draw
call**, e ci resta qualunque sia il numero di luoghi. È la stessa regola delle
calotte e dei confini: mai un oggetto three per elemento.

**Perché non il layer punti di globe.gl.** All'inizio i pin erano quelli, con
`pointsMerge(true)`, che la fusione la faceva già. Ma quel layer disegna
**cilindri** e non c'è modo di cambiarne la forma: da vicino si vedeva che erano
tubi. Le sfere quindi se le costruisce `buildPlaceMesh()`, che non è un gran
lavoro - una sferetta UV di 10×6 segmenti sono 120 triangoli, e con un centinaio
di luoghi restano briciole accanto ai 139.000 delle calotte.

Due dettagli che la rendono decente:

- **la finta ombreggiatura sta nei colori per-vertice.** Il materiale è non
  illuminato come quello delle calotte, quindi una sfera di colore piatto si
  vedrebbe come un cerchio; la luce si finge lungo la verticale locale, e la
  sferetta legge come una perlina appoggiata sul paese;
- **il centro sta a una volta il raggio sopra la calotta**, così la perlina
  poggia sulla superficie invece di sprofondarci o di levitare. È anche la quota
  a cui mirano il tocco e l'ancora della card, che devono puntare al centro
  della perlina e non alla superficie sotto.

Costruire la geometria noi ha un effetto collaterale utile: **il raggio può
seguire lo zoom con un passo fitto**. Con il layer punti, cambiare `pointRadius`
ricostruiva l'intero layer, quindi il valore andava quantizzato grossolanamente
(passi di 1,6) e il salto si vedeva; rifare qualche decina di sferette costa
niente, e il passo è sceso a 1,15, circa mezzo pixel per gradino.

`PLACE_ALT` e' 0,011, sopra tutte e tre le quote esistenti (calotte 0,002,
confini 0,005, contorno del selezionato 0,008) e sotto l'ancora della card
(0,014). Il pin e' un cilindro che parte dalla superficie, quindi attraversa la
calotta e si vede da sopra; a filo dell'orizzonte si vede di fianco e sembra uno
spillo, che e' esattamente quello che deve sembrare.

**Il raggio e' in pixel, non in gradi.** Un raggio angolare fisso e' il
compromesso peggiore possibile: lo stesso valore che a globo intero fa un
puntino di 3 px, a zoom massimo fa una macchia di 55 px che copre mezza Italia.
Verificato a schermo prima di correggerlo. Quindi il raggio angolare si ricalcola
dall'altitudine della camera:

> un pixel vale `2*(d-R)*tan(fov/2)/altezza` unita' di mondo alla superficie, e
> un grado d'arco ne vale `R*pi/180`

da cui il raggio che rende sempre `PLACE_PX` pixel. Il valore si **quantizza a
potenze di 1,6**, perche' assegnare `pointRadius` ricostruisce la geometria fusa
e durante una pinch succederebbe a ogni frame; con la quantizzazione cambia una
manciata di volte e il salto non si vede. L'aggiornamento vive dentro
`supervise()`, accanto ai piani di taglio, quindi segue il disegno su richiesta e
non gira a globo fermo.

**Il raycast va rispento a ogni aggiornamento.** L'oggetto fuso dei punti e'
*nuovo* ogni volta che i luoghi cambiano, quindi `restrictRaycast()` va
richiamato dopo ogni `pointsData()` e dopo ogni cambio di raggio - due volte, una
subito e una nel tick successivo, perche' il layer puo' ricostruire la geometria
in differita. Senza, il raggio di hover di globe.gl tornerebbe ad attraversarlo
venti volte al secondo, e un tocco sul pin finirebbe al layer punti invece che a
`onGlobeClick`.

### La perlina in prova

Toccare una città nella ricerca prima faceva volare il globo e basta: si
spostava, e la città non si vedeva. *Dov'è?* e *la voglio* sono due domande
diverse, e rispondere alla prima non deve salvare niente.

Quindi una città non ancora sul globo si mostra con una **perlina temporanea**:
vive solo in `tempPlace`, non in DataStore, ed è smorzata al 60% rispetto a una
salvata. Sparisce da sola quando la sua card si chiude, quando se ne apre
un'altra, o quando viene promossa a luogo vero.

Il pulsante della card è **uno solo con due significati**, decisi al momento del
tocco: *Rimuovi* se il luogo è salvato, *Metti sul globo* se è la perlina in
prova. Così la card non va ricostruita quando lo stato cambia sotto di lei -
cosa che succede sempre, perché aggiungere passa da Kotlin e torna indietro come
`setPlaces`.

Il resto del codice lavora su `allPlaces()`, che è i salvati più l'eventuale
perlina: così disegno e tocco la trattano come tutte le altre, mentre
`getPlaces()` e il salvataggio vedono solo i salvati e non c'è modo che una
perlina in prova finisca su disco per sbaglio.

### Prendere un pin col dito

Il pin non si tocca col raycast, come non ci si tocca il paese. Ma a differenza
del paese il confronto **non e' in gradi d'arco, e' in pixel**: un pin vicino al
bordo del globo e' schiacciato dalla prospettiva, e una soglia angolare lo
renderebbe un bersaglio enorme li' e minuscolo al centro. Si proietta ogni luogo
con `getScreenCoords` - la stessa camera che disegna, quindi non c'e' niente da
tenere allineato a mano - e si prende il piu' vicino entro `PLACE_PICK_PX`
(26 px: il pin disegnato e' piccolo, il dito no).

I luoghi dell'emisfero nascosto vanno scartati, altrimenti un tocco in Europa
potrebbe prendere un pin in Nuova Zelanda: proiettano a schermo lo stesso. Per
una sfera di raggio R centrata nell'origine, il punto P e' sulla calotta visibile
dalla camera C solo se **P.C >= R^2**.

I pin hanno la precedenza sul paese sotto: sono piccoli e ci si mira apposta,
mentre il paese si prende ovunque.

**Nota su `onGlobeClick`.** Il secondo argomento e' l'evento DOM, e la lat/lng
del primo arriva dal raycast di hover di globe.gl, che e' throttlato a 50 ms:
misurato, in un tocco sintetico quella coordinata e' ancora quella del tocco
*precedente*. Per il pin si usa `event.clientX/Y`, che e' sempre fresca. Con un
dito vero la differenza non si vede, ma se un giorno la selezione del paese
sbagliasse bersaglio su tocchi rapidi, la causa e' li'.

### Il luogo non tocca il paese

Piantare un pin a New York **non** segna gli Stati Uniti, e la card di un luogo
non ha l'interruttore *Visitato*: un luogo o c'e' o non c'e', e il pulsante e'
*Rimuovi*. Anche il pallino nella ricerca cambia significato - acceso vuol dire
"e' sul globo" - e per questo prende il bianco dei pin invece dell'arancione dei
visitati. Il contatore continua a contare solo i paesi.

Lato dati la separazione e' fisica: i luoghi stanno in una **chiave DataStore
nuova** (`places`, un array JSON), non dentro `visited_countries`. Aggiungere i
luoghi non poteva quindi rovinare i paesi gia' salvati, e non serviva nessuna
migrazione.

### I pin a mano

Un luogo che in `cities.json` non c'e' si pianta **tenendo premuto sul globo**,
oppure si scrive dalla ricerca quando non trova niente - stesso dialogo, cambia
solo cosa e' gia' compilato.

globe.gl non ha un evento di pressione lunga, e un timer sul `pointerdown` da
solo non basta: sul globo il gesto normale e' trascinare per ruotare, e con due
dita si zooma. Il rilevatore quindi annulla in tre casi, e sono tutti e tre
necessari:

- il dito si sposta oltre **10 px** (sta ruotando, non premendo);
- arriva un **secondo dito** (`isPrimary` falso): senza questo, una pinch
  aprirebbe il dialogo mentre si sta zoomando;
- `pointerup`, `pointercancel`, `pointerleave` o `wheel`.

Due dettagli che si scoprono solo provando: dopo che la pressione lunga e'
scattata arriva **comunque** il `pointerup`, e globe.gl lo tratterebbe come un
click selezionando il paese sotto - da qui `swallowNextClick`, che ne scarta uno
e uno solo. E premendo fuori dal globo `toGlobeCoords` non restituisce niente:
premere sul cielo non deve creare un luogo a caso.

**Il pin non nasce nel JavaScript.** La pressione lunga chiama
`onPlaceRequested(lat, lng, chiavePaese)` e si ferma li': il nome lo chiede
Compose e il luogo lo scrive DataStore, perche' la verita' non sta mai nella
WebView. La chiave del paese viaggia insieme alle coordinate proprio per non
doverla richiedere dopo.

Quando invece le coordinate le scrive l'utente, il paese non lo sa nessuno: li'
Compose interroga il globo con `BeenThere.countryAt()` attraverso
`resolveCountry`, che e' l'unico punto di tutta l'app in cui Kotlin **chiede**
qualcosa alla pagina invece di comandarla, e l'unico uso di `evaluateJavascript`
con callback. Vale la pena perche' il point-in-polygon vive li' e duplicarlo in
Kotlin significherebbe tenerne due allineati.

**Le coordinate si incollano intere.** Il campo accetta `41.9028, 12.4964` in un
colpo solo, separatori intorno compresi, e in seconda battuta anche la virgola
come decimale (`41,9 12,5`). Non e' un vezzo: l'app non ha rete, quindi le
coordinate arrivano per forza copiate da un'altra app, e obbligare a spezzarle in
due caselle renderebbe questa strada inservibile. Fuori intervallo si rifiuta.

Il parsing esiste in due copie, Kotlin e JavaScript, per la stessa ragione della
ricerca: il prototipo browser deve restare provabile per intero. La copia
sacrificabile e' quella JavaScript.

**Cosa manca ancora:** un pin creato a mano non si modifica, si cancella e si
rifa. La lista non ha ancora una sezione per i luoghi, quindi la via per
ritrovarne uno e' la ricerca, che infatti guarda anche fra i pin dell'utente -
altrimenti uno piantato per sbaglio in mezzo al Pacifico non si ritroverebbe.

## Verso degli anelli del GeoJSON

All'avvio `index.html` riavvolge in place i poligoni prima di passarli al globo,
e non è un dettaglio cosmetico. three-globe costruisce le calotte con
`three-conic-polygon-geometry`, che si appoggia a **d3-geo**, e d3-geo usa la
convenzione sferica **opposta a RFC 7946**: anello esterno in senso *orario*
(area sferica con segno positiva), buchi in senso antiorario. Il dataset è
RFC 7946, quindi tutti i poligoni avevano area d3 negativa e d3 li leggeva
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
24 col minimo a 0.15 e a 45 col minimo a 0.05. I 15 che restano sono territori
minuscoli (Guam, Isola di Man, Åland, Fær Øer, Cipro, Lussemburgo, Brunei).
Queste cifre dipendono dall'area dei poligoni, non dal dettaglio del contorno,
quindi non cambiano rigenerando il dataset a risoluzione diversa.

Il conto è migliaia di volte quello del default, ed è la voce che domina i ~2
secondi di costruzione del globo. Si paga **una volta sola**: appena le calotte
sono pronte vengono fuse e il layer poligoni viene smontato.

Il valore è memoizzato sulla feature perché l'accessor viene rieseguito a ogni
digest e un valore ballerino ritassellerebbe tutto. Oggi la tassellatura avviene
comunque **una volta sola**: appena pronta, le calotte vengono fuse e il layer
poligoni viene smontato.

## Una scena sola invece di migliaia di oggetti

È la modifica che ha spostato il framerate. globe.gl disegna ogni poligono come
un gruppo a sé, e un MultiPolygon ne produce uno per membro: i 242 paesi del
dataset sono **1.359 poligoni**, cioè altrettante mesh più altrettanti
`LineSegments`. Oltre 2.700 oggetti che three.js dovrebbe aggiornare, cullare e
ordinare a ogni frame, per circa 4.000 draw call. Su una GPU del 2019 il collo di
bottiglia non sono i triangoli — sono 138.971 in tutto, briciole — ma quel lavoro
per-oggetto.

C'era di peggio, e non si vedeva: globe.gl rilancia il **raycast del puntatore a
ogni tick**, con un throttle di 50 ms, quindi venti volte al secondo anche a dito
fermo, e quel raggio attraversava tutti gli oggetti. I `LineSegments` sono i più
cari da provare, perché il test va fatto segmento per segmento.

Ora il layer poligoni serve una volta sola, come fabbrica di geometrie: si
prendono le calotte già tassellate, si fondono in **una** `BufferGeometry` di
93.489 vertici con colore per-vertice, e `polygonsData([])` smonta tutto il
resto. In scena restano la sfera dell'oceano, l'atmosfera, le calotte, la rete
dei confini e il contorno del selezionato: **4 draw call** per frame, misurate —
e restano quattro qualunque sia la risoluzione del dataset.

I costruttori di three non sono esportati dal bundle di globe.gl — anzi, il
bundle guarda se `window.THREE` esiste per riusarlo, e non esiste — quindi
`harvestThree()` li recupera dagli oggetti che la libreria ha appena creato. Per
`BufferGeometry` serve risalire la catena dei prototipi: le geometrie di
three-globe sono sottoclassi con costruttori che pretendono argomenti.

**Ricolorare** non tocca più né geometria né scena: si riscrivono i float
dell'attributo colore nell'intervallo di vertici di quel paese e si chiede un
frame. Ogni feature ha il proprio `{ start, count }`, quindi segnare l'Italia non
sfiora la Francia. Prima ogni toggle rientrava nel digest di globe.gl su tutti i
gruppi.

I colori per-vertice three.js li usa **in spazio lineare** così come sono, mentre
`material.color.set('#8a8a8a')` passa per la conversione sRGB→lineare della
gestione colore. Per non ritrovarsi grigi slavati, i valori si ricavano facendo
il giro da un `THREE.Color`: qualunque sia la gestione colore attiva, escono
esattamente i numeri che la libreria avrebbe usato.

Le calotte sono disegnate a `FrontSide`. Dopo il riavvolgimento per d3-geo i
triangoli guardano fuori dalla sfera, quindi il culling scarta l'emisfero
nascosto prima di rasterizzarlo. Se un giorno le calotte sparissero, il primo
sospettato è `CAP_SIDE`: rimetterlo a `2` (DoubleSide) le fa tornare.

## Un confine solo per frontiera

Prima ogni paese portava il proprio contorno, e lungo una frontiera il contorno
di A e quello di B erano geometrie **coincidenti**. Non è una stima: sui 39.763
spigoli del dataset, **7.323 compaiono esattamente due volte** — e mai tre, il
che dice che la topologia è condivisa e i due spigoli hanno gli stessi identici
vertici. Mapshaper la preserva, quindi una chiave canonica sui due estremi li
riconosce senza tolleranze geometriche. È una proprietà da riverificare a ogni
rigenerazione del dataset: vedi l'avvertenza in *Il dataset*.

Oggi i confini sono **una sola** `LineSegments` per tutto il pianeta, costruita
deduplicando gli spigoli: 32.444 segmenti, circa 760 KB di vertici, una draw
call. Il contorno del paese selezionato è una seconda geometria che contiene i
contorni di tutti i paesi con i vertici di ognuno contigui: accenderne uno costa
un `setDrawRange`, senza costruire o buttare niente a ogni selezione.

Gli spigoli vengono suddivisi a passi di 1,5 gradi perché la linea segua la
curvatura invece di tagliare una corda che sprofonda nella sfera. Costa quasi
nulla: lo spigolo più lungo del dataset misura 5,65 gradi, quindi 32.440 spigoli
unici diventano 32.444 segmenti. L'unico caso da scartare a mano è quello di
chiusura
dell'Antartide, da `(-180,-90)` a `(180,-90)`: i due estremi sono lo stesso punto
sulla sfera, il polo sud, ma interpolarli in lat/lng farebbe il giro del mondo.

Sono spariti anche i **muri laterali**. Erano trasparenti (`polygonSideColor` a
`rgba(0,0,0,0)`) ma esistevano, e scrivevano nel depth buffer: lungo ogni
frontiera i muri di due paesi arrivavano alla stessa quota. Nel layer poligoni di
globe.gl 2.46.2 un colore di fianco *falsy* vale `includeSides = false`, quindi
passare `false` invece di un colore trasparente non li nasconde: non li
costruisce proprio. Stessa cosa per `polygonStrokeColor`.

## Perché i confini sfarfallavano

Il doppio disegno era solo metà della storia. L'altra metà è il **depth buffer**.

globe.gl imposta `camera.near = 0.05` e lascia `far` al valore di default. Con
quella coppia il quanto di profondità a distanza `z` vale circa
`z² / (near · 2^bit)`. Col globo inquadrato da lontano (z ≈ 340) fanno **0,14
unità** con un depth buffer a 24 bit e **35 unità** se il dispositivo ne alloca
uno a 16. Il contorno galleggiava sopra la calotta di 1e-4 di scala, cioè 0,01
unità: quattordici volte meno del quanto nel caso buono, tremila volte meno nel
caso cattivo. Il confronto di profondità fra calotta e contorno era rumore, e il
vincitore cambiava a chiazze. È anche il motivo per cui il difetto **peggiorava
rimpicciolendo il globo**: la precisione va col quadrato della distanza.

La cura è `updateClipPlanes()`, che stringe `near` e `far` attorno al guscio che
contiene globo e atmosfera a ogni movimento di camera. Il rapporto `far/near`
resta piccolo a ogni zoom — misurato: 17 a globo intero, 2,2 dopo un volo — e il
quanto scende sotto le 0,005 unità.

Il confronto va fatto sui **valori**, non sulla distanza della camera. Una
guardia del tipo "la distanza non è cambiata, non faccio niente" sembra
ragionevole e invece è un bug: se qualcun altro rimette mano ai piani non te ne
accorgi mai, e restano larghi proprio a globo fermo, cioè quando si guardano i
confini.

Con i piani stretti le quote (`CAP_ALT`, `BORDER_ALT`, `SEL_ALT`) possono stare
vicine. All'inizio erano distanziate di 0,3 unità l'una dall'altra, sessanta
volte il quanto peggiore, e da lì due conseguenze:

- **il paese selezionato non viene più sollevato.** `SEL_LIFT` serviva a
  scavalcare i muri laterali dei vicini e a battere il quanto di profondità;
  senza muri e con i piani stretti non serve più, e il paese non fa più il
  saltino quando lo si tocca;
- **il contorno bianco è continuo.** Prima si accendeva a segmenti alterni.

**Ma quei 0,3 si pagavano sul bordo del globo**, e l'utente se n'è accorto usando
l'app: a filo dell'orizzonte il confine si vedeva palesemente staccato dal
paese. Al centro del disco si guarda la superficie di faccia e l'altezza non si
distingue; verso il bordo la si guarda di taglio, e un'altezza `h` diventa uno
spostamento **laterale** di circa `sqrt(2*R*h)` — con 0,3 unità fanno 7,7 unità
d'arco, oltre 4 gradi.

Oggi la distanza è una costante sola, `SHELL_GAP`, e vale **0,001 di scala
(0,1 unità)**: lo scostamento scende a ~4,5 unità d'arco. Due cose da sapere
prima di rimetterci mano:

- **lo scostamento va con la radice dell'altezza.** Dividere la distanza per tre
  lo riduce del 40%, non di tre volte; per dimezzarlo bisogna dividere per
  quattro. Scendere ancora rende sempre meno;
- **il segnale di essere scesi troppo non è al centro, è lo sfarfallio dei
  confini a globo piccolo**, che è dove il quanto di profondità è peggiore. A
  0,1 unità si sta venti volte sopra il quanto misurato con i piani stretti, ma
  il margine non è più quello comodo di prima.

Se un giorno servisse azzerare del tutto la parallasse invece di attenuarla, la
strada è mettere confini e calotte alla **stessa** quota e separarli nel depth
buffer con `polygonOffset` sul materiale delle calotte: è la soluzione standard
per una decalcomania complanare, e sul bordo lo scostamento diventa zero per
costruzione.

## Il tocco non passa più dal raycast

Con la scena fusa le mesh su cui sparare un raggio sarebbero due o tre, e nessuna
corrisponde più a un singolo paese. Quindi calotte, confini e alone
dell'atmosfera vengono esclusi dal raycast (`restrictRaycast()`), e l'unico
bersaglio resta la sfera del globo: `onGlobeClick` fornisce la lat/lng, e il
paese lo decide un **point-in-polygon** sul GeoJSON, con un indice a griglia da 5
gradi. 1.619 celle occupate su 2.592, al massimo 25 poligoni candidati per
cella, mezzo microsecondo a tocco.

È anche più esatto del raycast, perché prova la sagoma vera invece della sua
triangolazione. L'unico scarto è la parallasse fra sfera e calotta, che sta 0,2
unità più in alto: a 80 gradi di incidenza vale 0,65 gradi d'arco, cioè solo a
filo dell'orizzonte, dove il paese è comunque schiacciato in pochi pixel.

Effetto collaterale su desktop: sparisce il cursore a manina *sul singolo paese*.
globe.gl aggiunge la classe `clickable` quando il puntatore sta sopra un oggetto
con un gestore di click, e ora quell'oggetto è il globo intero: la manina c'è su
tutto il globo, oceano compreso. Su Android non cambia niente.

## Disegno su richiesta e risoluzione adattiva

globe.gl tiene un `requestAnimationFrame` sempre acceso: ridisegna la scena e
rilancia il raycast anche quando sullo schermo non cambia un pixel. Su un
telefono è corrente sprecata, e la corrente sprecata diventa calore e quindi
throttling, cioè meno framerate proprio durante l'uso.

Ora il ciclo resta acceso finché c'è qualcosa da mostrare e si ferma dopo. Due
dettagli non ovvi:

- **la sveglia non può essere agganciata all'evento `change` dei controlli.** Col
  damping acceso OrbitControls continua a emetterlo per movimenti infinitesimi, e
  legarci il risveglio significa non addormentarsi mai. Si guarda invece lo
  spostamento vero della camera, con una soglia di 0,002 unità: inquadrando il
  globo a pieno schermo un pixel vale circa un quarto di unità, quindi si sta due
  ordini di grandezza sotto il visibile;
- **si tiene l'identificativo del frame, non un flag.** Ogni `wake()` annulla il
  frame in sospeso e ne chiede uno nuovo, così ce n'è sempre esattamente uno e la
  sorveglianza si riarma da sola. Con un flag booleano, un frame mai consegnato —
  il browser può sospenderli, per esempio a scheda nascosta — lascerebbe la
  sveglia bloccata per sempre.

Ogni evento di puntatore riaccende, quindi anche se una sveglia si perdesse
basterebbe toccare lo schermo.

La **risoluzione** parte da `min(2, devicePixelRatio)`, che è già quello che fa
globe.gl. Su un telefono con dpr 3 sono 2,25 volte meno pixel del pannello, ma su
una GPU del 2019 anche 720×1520 possono non bastare. Se il ritmo resta sotto i
~42 fps per un secondo intero si scende di un gradino (1,5 → 1,25 → 1). Non si
risale mai: un su-e-giù continuo si vedrebbe. Il testo non ne risente, perché i
nomi dei paesi li disegna Compose e la card del popup è HTML in un
`CSS2DRenderer`, non dentro il canvas WebGL.

`BeenThere.perf()` restituisce draw call, triangoli, risoluzione in uso e piani
di taglio: è la via per controllare come sta andando sul telefono da
`chrome://inspect`.

**Nota su `controls`.** Non impostare `rotateSpeed` e `zoomSpeed`: globe.gl li
ricalcola dall'altitudine a ogni evento `change` dei controlli, quindi qualsiasi
valore scritto all'avvio viene sovrascritto al primo movimento. `minDistance`,
`maxDistance`, `enablePan` e il damping invece restano.

I due limiti di zoom non si scrivono più in unità di mondo ma escono da
`MIN_ALT` e `MAX_ALT`, in altitudine, che è l'unità in cui ragiona tutto il
resto del file. **`MIN_ALT` è passato da 0,30 a 0,12 il 2026-09-09**, su
richiesta: piantare un pin con la pressione lunga vuol dire centrare un punto
col polpastrello, e a 0,30 un dito da 9 mm copriva ~90 km di superficie. La
scala a schermo va come `1/(d−R)`, quindi la superficie si ingrandisce di 2,5
volte e lo stesso dito ne copre ~36.

Due effetti collaterali che il conto da solo non mostra e che vanno guardati in
uno screenshot se un domani si scende ancora. Primo: sotto altitudine 0,25 il
`near` di `updateClipPlanes()` (`d − 1,25 R`) va sotto zero e si appoggia al suo
minimo di 0,5 — è la prima volta che quel minimo entra in gioco, e non fa danno
solo perché da vicino la geometria è vicina (a 0,12 l'orizzonte sta a ~50
unità, quanto di profondità sotto 3e‑4 a 24 bit). Secondo: a 112 la camera sta
**dentro** la sfera dell'atmosfera, che è a 1,15 R; regge perché è disegnata
sulle facce interne. Il vero limite però non è tecnico ma il dataset —
`simplify 40%`, `precision 0.001` — e a 0,12 la squadratura delle coste comincia
a vedersi.

Il volo della ricerca (`CITY_ALT`) resta a 0,32 e **non** segue il limite:
prima ci coincideva quasi, adesso è una scelta: atterrare dalla ricerca già allo
zoom massimo toglie il riferimento di dove si è finiti.

## La schermata di caricamento

Copre i ~2 secondi in cui il globo si costruisce. Tutto il suo disegno discende
da un unico vincolo: **la tassellatura blocca il thread principale**, quindi si
muove soltanto ciò che il compositor anima da sé, cioè `transform` e `opacity`.
Un'animazione su `background-position`, su un attributo SVG o guidata da JS si
inchioderebbe proprio mentre serve, e l'app sembrerebbe piantata.

Da lì tre scelte:

- il globo di attesa è **HTML statico** nel documento, quindi viene dipinto prima
  ancora che `globe.gl.min.js` venga eseguito: niente schermo nero iniziale;
- i **meridiani** sono una striscia con un motivo ripetuto che scorre. Trasla di
  esattamente un periodo del motivo (15px), così il ciclo si richiude su sé
  stesso e la giunta non si vede, qualunque sia la larghezza dell'elemento;
- i **paralleli** stanno fermi — ruotando attorno all'asse polare non si muovono
  davvero — e sono spaziati come `sin(latitudine)`, cioè 0, ±30, ±60 gradi. Sono
  loro, più della sfumatura, a far leggere una sfera invece di un cilindro.

Il trucco che regge l'illusione è la sfumatura sopra il reticolo. Su una sfera
vera i meridiani si stringono avvicinandosi al limbo; questi sono dritti e
paralleli, quindi proprio lì tradirebbero il cilindro. Due gradienti li spengono
prima che ci arrivino: uno radiale che scurisce il bordo in tutte le direzioni, e
uno orizzontale che insiste su destra e sinistra, i lati dove l'occhio se ne
accorgerebbe. Si è evitata una `mask` apposta: applicata a un antenato può far
uscire l'animazione del figlio dal percorso composito, che è esattamente ciò che
non deve succedere qui.

**Le due frasi di fase** (`Carico i confini…`, `Costruisco il globo…`) sono solo
due, e non c'è nessuna percentuale: la tassellatura è un blocco opaco dentro il
digest di globe.gl, e inventare un avanzamento sarebbe finto. La seconda frase va
scritta *prima* che il blocco cominci, e le va lasciato il tempo di arrivare a
schermo — due `requestAnimationFrame` di respiro. Ma non ci si può **dipendere**:
un browser che non sta producendo frame (una WebView non ancora visibile, una
scheda in background) non consegna `requestAnimationFrame`, e il globo non
partirebbe mai. Accanto c'è un `setTimeout` di 250 ms come rete: se i frame
arrivano si parte subito dopo il disegno, altrimenti si parte comunque.

`fail()` scrive in un elemento separato e spegne il globo di attesa. Prima
scriveva in `innerHTML` su `#status`, che oggi cancellerebbe il wireframe: i due
ruoli stanno in due elementi distinti apposta.

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

Il paese selezionato **non viene sollevato**. Lo era, con una costante
`SEL_LIFT`, perché senza si illuminavano solo alcuni segmenti del contorno: il
bianco vinceva il test di profondità a macchia di leopardo, contro i muri
laterali dei vicini e contro il contorno coincidente del paese confinante.
Tolti i muri e stretti i piani di taglio, la causa non c'è più e il sollevamento
nemmeno — così il paese non fa più il saltino quando lo si tocca. Il perché per
esteso sta in *Perché i confini sfarfallavano*.

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

## Il backup

L'app non ha rete e non sincronizza niente, quindi il file di backup è l'unica
copia dei dati che esce dal telefono e l'unica via per portarseli altrove.

**Contiene paesi e luoghi insieme.** Un backup dei soli pin lascerebbe fuori
metà del lavoro dell'utente, e se ne accorgerebbe solo dopo averne avuto
bisogno.

**L'import sostituisce**, previa conferma che dice cosa c'è nel file ("42 paesi
e 130 luoghi"). È l'unica operazione dell'app che può cancellare dati, quindi non
si scrive niente finché l'utente non ha visto quei due numeri; e la scrittura
è **una `edit` sola** su DataStore, così non esiste un istante in cui i paesi
sono quelli nuovi e i luoghi ancora quelli vecchi. Dopo l'import l'app è
esattamente il file: è il comportamento giusto per un ripristino, che unendo non
si avrebbe.

Si passa dal **selettore di file di sistema** (`CreateDocument` / `OpenDocument`
di `ActivityResultContracts`): nessun permesso da dichiarare, e il file finisce
dove vuole l'utente, Drive compreso. In lettura il tipo è lasciato aperto
(`*/*`) di proposito: parecchi gestori di file non mostrano i `.json` se si
filtra su `application/json`, e un backup che non si riesce a selezionare non
serve a niente. A dire se il file è buono ci pensa `Backup.decode`, che rifiuta
quello che non porta il marcatore `kind` giusto o che dichiara una versione più
nuova di quella che sappiamo leggere.

Il `version` in testa al file serve a questo: un domani il formato può cambiare
senza che i file vecchi diventino illeggibili. I luoghi dentro il backup usano lo
stesso `Place.toJson()` con cui stanno in DataStore — un formato solo, non due da
tenere allineati.

## Dettagli minori

**`[hidden]` ha bisogno di `!important`.** La regola `[hidden] { display: none }`
è del foglio di stile del browser, e qualsiasi regola d'autore la batte:
`#ui { display: flex }` vinceva, quindi su Android la chrome HTML restava a
schermo sopra la UI Compose, raddoppiando contatore e campo di ricerca.

**Ricerca.** Tornandoci si ricomincia da capo, invece di trovare la parola di
prima. Si azzera in **due momenti**, e servono entrambi: quando il gruppo perde
il fuoco, e quando il campo lo **riacquista**. Il secondo non è una ridondanza -
toccando il globo il fuoco se lo prende la WebView, che è una `View` Android
dentro una `AndroidView`, e Compose non sempre se ne accorge: con il solo
controllo sulla perdita di fuoco l'azzeramento non scattava mai. Il fuoco si
guarda sul **gruppo** e non sul solo campo di testo (`focusGroup()` più
`onFocusChanged`) perché nel pannello c'è anche il pallino di ogni riga: se
rubasse il fuoco al campo, un controllo sul solo campo chiuderebbe la lista
proprio mentre la si sta usando.

Cerca sul nome nella lingua selezionata più il codice ISO, senza
distinguere accenti. Per gli stati insulari troppo piccoli da colpire con il
dito, il pallino a destra di ogni risultato è la via di selezione. Le città
stanno in una sezione a parte sotto i paesi, non mescolate: sono 4.205 contro
242, e in una lista sola il paese cercato sparirebbe fra i suoi capoluoghi.

**La ricerca esiste due volte.** Quella vera è Compose; dentro `index.html` ce
n'è una seconda, accesa solo quando manca il ponte Android (`DEMO_UI`), perché
il prototipo nel browser resti provabile per intero. È l'unico posto in cui il
JavaScript carica `cities.json`: su Android quel file non passa mai dalla
WebView. Le due ricerche vanno tenute allineate a mano - stesso ordine, stessa
regola dell'inizio di parola, stesse sezioni - e la copia JS resta quella
sacrificabile se un giorno divergono.

**Lingua.** Selettore manuale IT/EN nella bottom sheet, salvato in DataStore. Non
passa dal locale di sistema perché `LocaleManager` è API 33+ e qui minSdk è 26:
le traduzioni stanno in `res/values` e `res/values-en`, lette da `appString()`
con un `Resources` a locale sovrascritto. La lingua deve arrivare anche dentro la
WebView, che disegna da sé nome e pulsante del popup:
`GlobeCommand.SetLanguage` la spinge con `BeenThere.setLanguage()`. Le stringhe
della pagina stanno nell'oggetto `STR` di `index.html`.

**Rotazione.** L'activity dichiara `configChanges`, così ruotando il telefono la
WebView non viene distrutta e il globo non si ricarica.
