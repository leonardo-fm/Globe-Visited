package com.beenthere.app.globe

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import com.beenthere.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject

/** URL locale servito da WebViewAssetLoader: nessuna richiesta esce dal dispositivo. */
private const val GLOBE_URL = "https://appassets.androidplatform.net/assets/index.html"

/**
 * Comandi che Kotlin manda al globo. Il globo non viene mai interrogato per
 * conoscere lo stato: quello vive in DataStore.
 */
class GlobeController {

    private var webView: WebView? = null

    internal fun attach(view: WebView) {
        webView = view
    }

    internal fun detach() {
        webView = null
    }

    /** Stato completo, usato all'avvio e dopo ogni ricreazione della WebView. */
    fun setVisited(codes: Set<String>) {
        eval("window.BeenThere && BeenThere.setVisited(${JSONArray(codes.toList())})")
    }

    /** Toggle originato dalla UI nativa (ricerca o lista). */
    fun setCountryVisited(code: String, isVisited: Boolean) {
        eval("window.BeenThere && BeenThere.setCountryVisited(${JSONObject.quote(code)}, $isVisited)")
    }

    fun focusCountry(code: String) {
        eval("window.BeenThere && BeenThere.focusCountry(${JSONObject.quote(code)})")
    }

    private fun eval(script: String) {
        webView?.evaluateJavascript(script, null)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GlobeWebView(
    controller: GlobeController,
    bridge: GlobeBridge,
    modifier: Modifier = Modifier
) {
    // Il bridge non deve cambiare identita' a ogni ricomposizione: la WebView lo
    // registra una volta sola nella factory.
    val stableBridge = remember { bridge }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
                .build()

            if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)

            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#05070F"))
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER

                with(settings) {
                    javaScriptEnabled = true
                    // Nulla di tutto questo serve: il globo e' solo WebGL su asset locali.
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    builtInZoomControls = false
                    displayZoomControls = false
                    setSupportZoom(false)
                    // Il testo dell'app e' tutto in Compose: la scala di sistema qui
                    // deformerebbe solo il layout della pagina.
                    textZoom = 100
                    mediaPlaybackRequiresUserGesture = true
                    cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
                }

                // WebViewClient di piattaforma: minSdk 26 e nessuna delle API compat
                // di androidx.webkit serve qui. WebViewAssetLoader funziona con entrambi.
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest
                    ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                    // La pagina non deve poter navigare da nessuna parte.
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean = true
                }

                addJavascriptInterface(stableBridge, "AndroidBridge")
                controller.attach(this)
                loadUrl(GLOBE_URL)
            }
        },
        onRelease = { view ->
            controller.detach()
            view.removeJavascriptInterface("AndroidBridge")
            view.destroy()
        }
    )
}
