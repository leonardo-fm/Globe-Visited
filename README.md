# Been There

Mappa personale dei paesi visitati, con globo 3D interattivo. Tutto offline:
l'app **non dichiara il permesso INTERNET** e non fa una sola richiesta di rete.

- Guscio nativo Kotlin + Jetpack Compose: contatore, ricerca, lista dei visitati.
- Globo reso in una WebView a schermo intero con [globe.gl](https://globe.gl) (three.js).
- Persistenza in DataStore Preferences. La verità sta lato Kotlin, non nella WebView.

## Gli asset del globo

`app/src/main/assets/` contiene gia' i due file necessari, versionati nel repo
perche' senza di loro l'app non funziona e non c'e' rete da cui prenderli a
runtime:

- `globe.gl.min.js` - build UMD standalone di globe.gl **2.46.2** (three incluso),
  1,8 MB, presa da `https://cdn.jsdelivr.net/npm/globe.gl@2.46.2/dist/globe.gl.min.js`
- `countries.geojson` - Natural Earth Admin 0 1:50m semplificato, 242 paesi, 193 KB

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
`ISO_A3 → ADM0_A3 → nome normalizzato`, ed è la stessa che
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
