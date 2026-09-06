# Been There

Mappa personale dei paesi visitati, su un globo 3D. Tutto offline: l'app **non
dichiara il permesso INTERNET** e non fa una sola richiesta di rete.

![Il globo](docs/globo.png)

![La card di un paese, con bandiera e interruttore Visitato](docs/popup.png)

## Cosa fa

- Tocchi un paese sul globo e si apre una card con bandiera, nome e un
  interruttore **Visitato**. La card resta agganciata al paese mentre ruoti.
- Contatore, ricerca e lista dei visitati nella UI nativa Compose.
- Italiano e inglese, a scelta manuale.
- I visitati stanno in DataStore, sul telefono. Niente account, niente cloud.

## Comandi

Installare sul telefono — Debug USB attivo e cavo collegato:

```
./gradlew installDebug
```

Solo l'APK, da passare a mano:

```
./gradlew assembleDebug
```

esce in `app/build/outputs/apk/debug/app-debug.apk`.

Provare il globo nel browser, senza Android:

```
cd app/src/main/assets
python -m http.server 8000
```

e apri `http://localhost:8000/index.html`. Non aprire il file con doppio clic: da
`file://` il browser blocca la lettura del GeoJSON.

Requisiti: JDK 17+ (testato con 21), compileSdk 35, minSdk 26. `local.properties`
con `sdk.dir` non è versionato: lo crea Android Studio, o lo scrivi a mano con
gli slash in avanti (`sdk.dir=C:/percorso/Android/Sdk`).

## Com'è fatto

```
app/src/main/assets/index.html          il globo: rendering, selezione, popup
app/src/main/java/com/beenthere/app/
  MainActivity.kt                       edge-to-edge, tema
  MainViewModel.kt                      stato unico + comandi verso il globo
  data/                                 modello, catalogo, ricerca, DataStore
  globe/                                il ponte con la WebView, nei due sensi
  ui/                                   Compose: contatore, ricerca, bottom sheet
```

Il globo è una WebView a schermo intero con [globe.gl](https://globe.gl), sotto
la UI Compose. A globo pronto il JavaScript passa a Kotlin l'intero catalogo dei
paesi in una chiamata sola, e da lì la ricerca è tutta nativa. La verità sui
visitati sta in DataStore, mai nella WebView.

globe.gl serve solo a costruire la geometria, una volta sola: all'avvio le 1.359
calotte vengono fuse in un'unica mesh e i confini in un'unica linea, con ogni
frontiera disegnata una volta invece di due. Il pianeta intero costa **4 draw
call** per frame, il ciclo di disegno si ferma a globo fermo e la risoluzione
scende da sola se il telefono non tiene il passo.

Costruirlo richiede un paio di secondi, e nel frattempo si vede un globo
wireframe che gira. È animato solo con `transform`, perché la tassellatura blocca
il thread principale e solo il compositor continua a muovere qualcosa.

## Gli asset

`app/src/main/assets/` contiene tre file versionati nel repo, perché senza di
loro l'app non parte e non c'è rete da cui prenderli:

| file | cos'è |
|---|---|
| `globe.gl.min.js` | globe.gl 2.46.2, build UMD con three incluso, 1,8 MB |
| `countries.geojson` | Natural Earth 1:50m semplificato, 242 paesi in 1.359 poligoni, 719 KB (compresso nell'APK) |
| `TwemojiCountryFlags.woff2` | font con i soli glifi delle bandiere, 78 KB, MIT |

## Note tecniche

Il perché delle scelte non ovvie — verso degli anelli del GeoJSON, tassellatura
della calotta, la scena fusa, i confini deduplicati, i piani di taglio che
tolgono lo sfarfallio, il tocco senza raycast, bandiere su Windows, il Caspio —
sta in [docs/note-tecniche.md](docs/note-tecniche.md). Da leggere prima di
toccare il rendering.
