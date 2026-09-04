# Been There — progetto Android

## Contesto

App Android nativa, completamente offline, che mostra un globo 3D su cui l'utente
colora i paesi visitati. La fase 1 (`app/src/main/assets/index.html`, già scritto) è un
prototipo autonomo verificabile nel browser: globo, click-to-toggle, ricerca, contatore.
Questo piano copre la fase 2, cioè il guscio Kotlin/Compose attorno a quel prototipo, e
le modifiche all'HTML necessarie perché la sorgente di verità passi a DataStore.

**Prerequisito:** il piano presuppone che tu abbia scaricato i due asset e confermato nel
browser che globo, click e colori funzionano. Se il test desktop rivela problemi, si
sistemano prima di partire.

## Requisiti consolidati

Dalla specifica iniziale, più le decisioni prese insieme:

| Ambito | Decisione |
|---|---|
| Dataset | Natural Earth **50m** semplificato con mapshaper (~242 feature) |
| Antartide | Presente e cliccabile come qualsiasi altro poligono |
| Territori | Ogni feature è un'entità a sé: cliccabile e contata singolarmente |
| Contatore | Denominatore = numero di poligoni del dataset (dinamico, non 195) |
| Caricamento | `WebViewAssetLoader` su `https://appassets.androidplatform.net/`, **nessun permesso INTERNET** |
| Ricerca | Nativa in Compose. Tocco sulla riga = vola sul paese; pulsante separato = toggle visitato |
| Lingua | Selettore manuale IT/EN, default italiano, cambia nomi paesi **e** stringhe UI |
| Lista visitati | Bottom sheet che si apre dal contatore |
| Guscio | Edge-to-edge, entrambi gli orientamenti |
| Identità | `com.beenthere.app`, label "Been There" |
| Persistenza | DataStore Preferences: `Set<String>` di codici + lingua scelta |

Non si aggiunge altro: niente reset globale, niente export/import, niente account.

## Asset da preparare (a tuo carico)

```
# globe.gl UMD standalone (three incluso) -> app/src/main/assets/globe.gl.min.js
https://cdn.jsdelivr.net/npm/globe.gl/dist/globe.gl.min.js

# Natural Earth Admin 0, 1:50m
https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson

npm install -g mapshaper
mapshaper ne_50m_admin_0_countries.geojson -filter-fields ISO_A3,ISO_A3_EH,ADM0_A3,SOV_A3,ISO_A2,NAME,NAME_LONG,NAME_IT,ADMIN -simplify visvalingam 8% keep-shapes -clean -o format=geojson precision=0.01 countries.geojson
```

`countries.geojson` va in `app/src/main/assets/`. Peso atteso 700 KB – 1 MB.

## Architettura

```
MainActivity (edge-to-edge)
 └── BeenThereScreen
      ├── GlobeWebView  (AndroidView)      <- il globo, unico consumatore di WebGL
      ├── CounterChip   (in alto a sx)     <- apre la bottom sheet
      ├── SearchField + risultati          <- ricerca nativa sul catalogo
      └── VisitedSheet  (ModalBottomSheet) <- elenco visitati + selettore IT/EN
```

**Flusso dei dati.** All'avvio la WebView carica il globo e chiama
`AndroidBridge.onGlobeReady(catalogJson)` passando l'intero catalogo dei paesi
(`code`, `nameIt`, `nameEn`) in un colpo solo. Compose costruisce da lì l'indice di
ricerca e il denominatore del contatore, e non deve più interrogare il JS per cercare.
Il ViewModel spinge lo stato iniziale con `BeenThere.setVisited([...])`.

Da lì in poi:
- **tocco sul globo** → JS colora subito, poi `AndroidBridge.onCountryToggled(code, visited)` → ViewModel persiste. Kotlin non ri-pusha lo stato (eviterebbe un loop di ridisegni).
- **toggle da ricerca o da lista** → ViewModel persiste, poi `evaluateJavascript("BeenThere.setCountryVisited('ITA', true)")`.
- **selezione da ricerca** → `evaluateJavascript("BeenThere.focusCountry('MLT')")`.

Compromesso da conoscere: la barra di ricerca resta inerte finché la WebView non ha
finito di caricare (circa un secondo). L'alternativa — pre-generare un `countries.json`
a build time — è esclusa perché il GeoJSON lo scarichi tu e non è nel repo. Nel frattempo
il campo mostra un placeholder disabilitato.

## File da creare

**Build**
- `settings.gradle.kts`, `build.gradle.kts` (root), `gradle.properties`, `.gitignore`
- `gradle/libs.versions.toml` — version catalog
- `app/build.gradle.kts` — compileSdk 35, minSdk 26, targetSdk 35, Kotlin 2.x con il plugin `org.jetbrains.kotlin.plugin.compose`, `buildFeatures { compose = true }`, niente viewBinding
- `app/proguard-rules.pro` — **regola `-keep` sulla classe del bridge**: R8 in release rinomina i metodi `@JavascriptInterface` e il ponte smette di funzionare senza errori visibili

Dipendenze: `androidx.core:core-ktx`, `lifecycle-runtime-ktx`, `activity-compose`,
Compose BOM (ui, material3, material-icons), `androidx.datastore:datastore-preferences`,
`androidx.webkit` (per `WebViewAssetLoader`). Il parsing del catalogo usa `org.json`,
già in Android: nessuna dipendenza di serializzazione in più.

Nota sulle versioni: fisso un set noto-funzionante (AGP 8.7.x / Kotlin 2.0.x / Compose BOM
2024.10.x). Non posso verificare offline quali siano le ultime uscite: dopo il primo
sync lascia che Android Studio proponga gli aggiornamenti.

Il `gradle-wrapper.jar` è un binario che non posso generare: scrivo
`gradle/wrapper/gradle-wrapper.properties` e il wrapper lo completi con
`gradle wrapper` o aprendo il progetto in Android Studio.

**Manifest e risorse**
- `app/src/main/AndroidManifest.xml` — nessun `<uses-permission>`, `android:configChanges="orientation|screenSize|smallestScreenSize|screenLayout|keyboardHidden|uiMode"` sull'activity
- `res/values/strings.xml` (italiano) e `res/values-en/strings.xml` (inglese)
- `res/values/themes.xml`, `colors.xml` — tema scuro fisso sui colori della specifica
- Icona launcher adattiva vettoriale (`mipmap-anydpi-v26/ic_launcher.xml` + foreground/background), nessun PNG binario

**Kotlin** (`app/src/main/java/com/beenthere/app/`)
- `MainActivity.kt` — `enableEdgeToEdge()`, tema, `BeenThereScreen`
- `data/SettingsRepository.kt` — DataStore: `stringSetPreferencesKey("visited")`, `stringPreferencesKey("language")`; espone `Flow`
- `data/Country.kt` — modello `Country(code, nameIt, nameEn)` + parsing del catalogo JSON
- `globe/GlobeBridge.kt` — classe con i due metodi `@JavascriptInterface`. **Girano su un thread di background**: ogni callback rimbalza sul main scope prima di toccare stato o WebView
- `globe/GlobeWebView.kt` — `AndroidView` che costruisce la WebView, registra `WebViewAssetLoader` in `shouldInterceptRequest`, abilita JavaScript e WebGL, disabilita zoom nativo e text autosizing; espone un `GlobeController` con `setVisited/setCountryVisited/focusCountry`
- `MainViewModel.kt` — stato unico: `visited`, `catalog`, `language`, `ready`
- `ui/BeenThereScreen.kt`, `ui/CounterChip.kt`, `ui/SearchField.kt`, `ui/VisitedSheet.kt`
- `ui/Localization.kt` — vedi sotto
- `ui/theme/Color.kt`, `Theme.kt`

**Modifiche a `assets/index.html`** (già scritto in fase 1)
- `DEMO_UI = false`, `DEMO_PERSISTENCE = false`: spariscono chrome HTML e localStorage
- `onGlobeReady` passa il catalogo completo (`code`, `nameIt`, `nameEn`) invece del solo conteggio
- il resto — funzione unica di identificazione, altitudine costante, repaint solo-colore — resta intatto

## Punti delicati

**Nessun permesso INTERNET.** `WebViewAssetLoader` intercetta le richieste verso
`appassets.androidplatform.net` e le serve dagli asset locali: non tocca mai la rete.
Il manifest resta senza `<uses-permission>`.

**Ricolore efficiente.** `polygonAltitude` è una costante e
`polygonsTransitionDuration(0)`: three-globe rigenera la geometria solo se cambia
l'altitudine, quindi un toggle riesegue soltanto l'accessor del colore. Vale sia per il
tocco sul globo sia per il toggle che arriva da Kotlin.

**Chiave paese.** `ISO_A3` → `ISO_A3_EH` → `ADM0_A3` → `SOV_A3` → nome normalizzato,
funzione unica lato JS. I codici salvati in DataStore vengono da lì, quindi restano
stabili finché non cambi dataset. Il warning in console sulle chiavi duplicate resta
attivo come rete di sicurezza.

**Rotazione.** Con `configChanges` l'activity non viene ricreata: la WebView sopravvive e
il globo non si ricarica ruotando il telefono. Il prezzo è che i cambi di configurazione
li gestisci a mano — qui non serve nulla, perché la lingua è manuale e il tema è fisso.

**Lingua manuale su API 26.** Il selettore non può passare per il locale di sistema
(`LocaleManager` è API 33+). Uso un `Context` con configurazione locale sovrascritta,
fornito via `CompositionLocalProvider(LocalContext, LocalConfiguration)`: così
`strings.xml` e `values-en/` restano il posto dove vivono le traduzioni e il cambio è
istantaneo, senza ricreare l'activity e senza dipendere da appcompat. Se il tuo Compose
risolvesse le stringhe da `LocalResources` ignorando l'override, il ripiego è una mappa
Kotlin di stringhe: stesso risultato, ma le traduzioni escono da `strings.xml`.

**Ricerca.** Normalizzazione con `java.text.Normalizer` (NFD + rimozione dei diacritici),
match sui prefissi di parola prima dei match interni. Cerca sul nome nella lingua
selezionata più il codice ISO; i nomi nell'altra lingua non fanno match, come chiesto —
è una riga da cambiare se ci ripensi. Dove `NAME_IT` manca nel dataset, l'etichetta
italiana ricade sul nome inglese.

## Verifica

1. `./gradlew assembleDebug` — build pulita.
2. Permessi: `grep -i permission app/build/intermediates/merged_manifests/debug/AndroidManifest.xml` non deve restituire nulla. Controprova sul device: modalità aereo attiva, l'app funziona identica.
3. Globo: tocco su Italia → arancione, tocco di nuovo → grigio. Tocco su Kosovo, Cipro del Nord, Somaliland, Francia e Norvegia → si colora **solo** quello toccato.
4. Contatore: coerente dopo ogni toggle, da qualunque via (globo, ricerca, lista).
5. Persistenza: segna 3 paesi, chiudi l'app dai recenti, riapri → i 3 sono ancora arancioni.
6. Ricerca: `malta` e `singapore` trovati e selezionabili dal pulsante di toggle senza mai toccarli sul globo; `costa d'avorio` trovato nonostante l'apostrofo.
7. Lingua: passa a EN dalla bottom sheet → contatore, placeholder, lista e nomi dei paesi cambiano insieme; riapri l'app e la scelta è ricordata.
8. Rotazione: ruota il telefono → il globo non si ricarica, mantiene posizione e colori.
9. Release: `./gradlew assembleRelease` e prova il toggle su quella build — verifica che R8 non abbia rotto il bridge.
