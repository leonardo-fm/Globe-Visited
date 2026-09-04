package com.beenthere.app.globe

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

/**
 * Ponte JS -> Kotlin, esposto alla pagina come `AndroidBridge`.
 *
 * I metodi annotati con @JavascriptInterface girano su un thread di servizio
 * della WebView, mai sul main thread: qui rimbalzano tutti sul main prima di
 * toccare stato o WebView.
 *
 * Nota per la build release: i nomi dei metodi sono cercati dal JavaScript per
 * stringa, quindi R8 non deve rinominarli. La regola sta in proguard-rules.pro.
 */
class GlobeBridge(
    private val onReady: (catalogJson: String) -> Unit,
    private val onToggled: (code: String, isVisited: Boolean) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    /** Chiamato una volta a globo pronto, con l'intero catalogo dei paesi. */
    @JavascriptInterface
    fun onGlobeReady(catalogJson: String) {
        main.post { onReady(catalogJson) }
    }

    /** Tocco su un paese del globo: il JS ha gia' colorato, qui si persiste. */
    @JavascriptInterface
    fun onCountryToggled(code: String, isVisited: Boolean) {
        main.post { onToggled(code, isVisited) }
    }
}
