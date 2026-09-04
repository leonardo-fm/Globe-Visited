# Been There

Mappa personale dei paesi visitati, con globo 3D interattivo. Tutto offline:
l'app **non dichiara il permesso INTERNET** e non fa una sola richiesta di rete.

- Guscio nativo Kotlin + Jetpack Compose: contatore, ricerca, lista dei visitati.
- Globo reso in una WebView a schermo intero con [globe.gl](https://globe.gl) (three.js).
- Persistenza in DataStore Preferences. La verità sta lato Kotlin, non nella WebView.

## Prima della compilazione: i due asset

Non sono nel repository e vanno scaricati a mano in `app/src/main/assets/`.

**1. globe.gl (build UMD standalone, three incluso)**

```
https://cdn.jsdelivr.net/npm/globe.gl/dist/globe.gl.min.js
```

Salvalo come `app/src/main/assets/globe.gl.min.js`. Guarda quale versione ti ha
risolto jsDelivr (è scritta nel commento in testa al file) e riscarica con l'URL
pinnato, per esempio `.../globe.gl@2.35.3/dist/globe.gl.min.js`.

**2. Confini dei paesi (Natural Earth Admin 0, 1:50m)**

```
https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson
```

Semplificalo con [mapshaper](https://github.com/mbloch/mapshaper):

```
npm install -g mapshaper

mapshaper ne_50m_admin_0_countries.geojson -filter-fields ISO_A3,ISO_A3_EH,ADM0_A3,SOV_A3,ISO_A2,NAME,NAME_LONG,NAME_IT,ADMIN -simplify visvalingam 8% keep-shapes -clean -o format=geojson precision=0.01 countries.geojson
```

Il risultato va in `app/src/main/assets/countries.geojson`, peso atteso 700 KB – 1 MB.

Perché il 50m e non il 110m della prima bozza: a 1:110m Malta, Singapore e
Maldive **non esistono come poligoni**, quindi non sarebbero né cliccabili né
cercabili. Restano fuori anche dal 50m soltanto Vaticano, Monaco, San Marino,
Liechtenstein, Andorra e alcune isole del Pacifico, presenti solo a 1:10m.

`keep-shapes` impedisce che le isole minori spariscano nella semplificazione,
`-clean` ripara le auto-intersezioni che la semplificazione introduce,
`precision=0.01` (~1 km) taglia i decimali inutili.

## Compilazione

Il `gradle-wrapper.jar` non è versionato: generalo una volta con

```
gradle wrapper
```

oppure apri semplicemente il progetto in Android Studio, che lo crea da solo.
Poi:

```
./gradlew assembleDebug
```

Requisiti: JDK 17+, compileSdk 35, minSdk 26.

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
app/src/main/assets/index.html          il globo: rendering, tocchi, colori
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
catalogo dei paesi in una sola chiamata; da lì la ricerca è tutta nativa. I
tocchi sul globo colorano subito lato JS e vengono solo persistiti da Kotlin;
i toggle nati dalla UI nativa vengono persistiti e poi spinti al globo con
`evaluateJavascript`. Non c'è mai un giro di ritorno che ridisegni due volte.

## Punti che è utile conoscere

**Identificazione dei paesi.** In Natural Earth alcuni stati hanno
`ISO_A3 = "-99"` (Kosovo, Cipro del Nord, Somaliland, e in certe release anche
Francia e Norvegia). Una funzione unica in `index.html` risolve la chiave con
`ISO_A3 → ISO_A3_EH → ADM0_A3 → SOV_A3 → nome normalizzato`, ed è la stessa che
produce i codici salvati in DataStore. All'avvio la console logga un warning se
due feature collidono sulla stessa chiave.

**Ricolore efficiente.** L'altitudine dei poligoni è una costante e le
transizioni sono a zero: three-globe rigenera la geometria solo quando cambia
l'altitudine, quindi un tocco riesegue soltanto l'accessor del colore.

**Contatore.** Il denominatore è il numero di poligoni presenti nel dataset
(~242), non 195: Natural Earth include territori non sovrani e non coincide con
la lista ONU. Ogni feature è cliccabile e contata singolarmente.

**Ricerca.** Cerca sul nome nella lingua selezionata più il codice ISO, senza
distinguere accenti. Per gli stati insulari troppo piccoli da colpire con il
dito, il pallino a destra di ogni risultato è la via di selezione.

**Lingua.** Selettore manuale IT/EN nella bottom sheet, salvato in DataStore.
Non passa dal locale di sistema perché `LocaleManager` è API 33+ e qui minSdk è
26: le traduzioni restano in `res/values` e `res/values-en`, lette da
`appString()` con un `Resources` a locale sovrascritto.

**Rotazione.** L'activity dichiara `configChanges`, così ruotando il telefono la
WebView non viene distrutta e il globo non si ricarica.
