---
name: ambiente-senza-toolchain-android
description: "L'SDK Android ORA c'e': da questa macchina si compila con ./gradlew, e l'S10e e' collegato via adb - i crash si leggono da qui, non li detta l'utente"
metadata:
  node_type: memory
  type: project
  originSessionId: 41cb3d5f-b93c-4509-833e-f6ccfe5d5cb4
  modified: 2026-09-08T00:00:00.000Z
---

**Corretto il 2026-09-08: l'SDK Android c'e'.** `local.properties` punta a
`C:/Users/Lo/AppData/Local/Android/Sdk`, la cartella esiste con `platforms`,
`build-tools` e `licenses`, e c'e' JDK 21 (Temurin). Da qui:

```
./gradlew compileDebugKotlin     # ~30s, prende gli errori Kotlin
./gradlew assembleDebug          # l'APK intero
```

Fino a questa data la nota diceva il contrario e prescriveva controlli manuali
al posto del compilatore.

**Perche' conta:** fidandosi della vecchia nota sono state consegnate due volte
modifiche che non compilavano, e l'utente ha dovuto fare da postino incollando
gli errori: un apostrofo non protetto in `strings.xml` e un
`import androidx.compose.ui.focus.focusGroup` che in Compose 1.7 sta invece in
`androidx.compose.foundation`. Nessuno dei due sarebbe passato da un
`compileDebugKotlin`.

**Come applicarlo:** compilare **prima** di dire che una modifica Kotlin e'
pronta, sempre. I controlli manuali (parentesi bilanciate, `R.string` usate
contro dichiarate, import orfani) restano utili come primo giro veloce, ma non
sostituiscono il compilatore. Quello che resta davvero non verificabile da qui
e' il **comportamento a schermo**: per quello serve l'S10e.

**2026-09-09: l'S10e e' collegato via adb.** Verificato:

```
C:/Users/Lo/AppData/Local/Android/Sdk/platform-tools/adb.exe devices -l
# RF8MB1K9D7T  device  model:SM_G970F

adb logcat -b crash -d      # lo stack trace vero dell'ultimo crash
```

**Perche' conta:** fino a quel giorno i crash arrivavano descritti a parole
("appena esporto, crasha tutto") e si andava per ipotesi. Il primo uso di
`logcat -b crash` ha dato la riga esatta in tre secondi e ha smentito l'ipotesi
su cui stavo per lavorare - vedi [[diagnosi-utente-parziali]]. **Davanti a un
crash sul telefono la prima mossa e' leggere il logcat, non leggere il codice.**

Installare o lanciare l'app da qui resta pero' cosa sua: l'ha chiesto
esplicitamente, e `adb install` non fa eccezione senza chiederglielo prima.

Vedi [[been-there-stato-lavoro]] e [[bash-heredoc-mangia-backslash]].
