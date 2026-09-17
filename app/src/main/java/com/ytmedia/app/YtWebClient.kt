package com.ytmedia.app

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

open class YtWebClient(private val onUrl: (String) -> Unit = {}) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?, request: WebResourceRequest?
    ): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        if (AdBlocker.shouldBlock(url)) return AdBlocker.blockedResponse()
        return null
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val u = request?.url?.toString() ?: return false
        val host = request.url.host ?: ""
        val allowed = host.endsWith("youtube.com") || host.endsWith("youtu.be") ||
            host.endsWith("google.com") || host.endsWith("googleusercontent.com") ||
            host.endsWith("gstatic.com") || host.endsWith("ggpht.com") ||
            host.endsWith("googlevideo.com") || host.endsWith("accounts.youtube.com")
        if (!allowed) {
            runCatching {
                view?.context?.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW, request.url)
                )
            }
            return true
        }
        onUrl(u)
        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        view?.evaluateJavascript(Injector.AD_SKIP_JS, null)
        url?.let(onUrl)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.evaluateJavascript(Injector.AD_SKIP_JS, null)
        view?.evaluateJavascript(Injector.UI_POLISH_JS, null)
        view?.evaluateJavascript(Injector.PLAYER_BRIDGE_JS, null)
        url?.let(onUrl)
    }
}
