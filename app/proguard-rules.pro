# Il ponte JS<->Kotlin viene chiamato per nome dal JavaScript della WebView.
# Senza questa regola R8 rinomina i metodi in release e il ponte smette di
# funzionare in silenzio: nessun crash, semplicemente i tocchi sul globo non
# vengono piu' salvati.
-keepclassmembers class com.beenthere.app.globe.GlobeBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.beenthere.app.globe.GlobeBridge { *; }
