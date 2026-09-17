package com.ytmedia.app

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Hardened, cached, lightweight WebView tuned for the YouTube web client.
 */
@SuppressLint("SetJavaScriptEnabled")
class YtWebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : WebView(context, attrs) {

    companion object {
        const val UA_MOBILE =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    init {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            userAgentString = UA_MOBILE
            cacheMode = WebSettings.LOAD_DEFAULT      // aggressive HTTP cache reuse
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            textZoom = 100
        }
        setLayerType(LAYER_TYPE_HARDWARE, null)
        overScrollMode = OVER_SCROLL_NEVER
        isScrollbarFadingEnabled = true
        setBackgroundColor(0xFF0F0F0F.toInt())

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@YtWebView, true)
        }
    }
}
