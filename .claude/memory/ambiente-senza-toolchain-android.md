---
name: ambiente-senza-toolchain-android
description: "Su questa macchina manca la toolchain Android, quindi il codice Kotlin non si puo' compilare da qui"
metadata: 
  node_type: memory
  type: project
  originSessionId: 41cb3d5f-b93c-4509-833e-f6ccfe5d5cb4
  modified: 2026-09-04T19:08:27.175Z
---

Su questa macchina (Windows 11) c'e' solo **JDK 21 (Temurin)**: `gradle` non e'
nel PATH, `ANDROID_HOME` e `ANDROID_SDK_ROOT` sono vuoti, e non esiste
`%LOCALAPPDATA%\Android\Sdk`. Node e Python invece ci sono.

**Perche' conta:** il codice Kotlin/Compose di Been There non e' verificabile da
qui. La compilazione la fa l'utente in Android Studio.

**Come applicarlo:** non promettere che il codice compili. Prima di consegnare,
sostituire il compilatore con controlli manuali: `node --check` sul JS estratto
dall'HTML, bilanciamento delle parentesi sui file Kotlin, confronto fra le
`R.string.*` usate e quelle dichiarate, verifica che firme e call site dei
composable coincidano. Dire chiaramente cosa e' stato verificato e cosa no.

Vedi [[been-there-stato-lavoro]].
