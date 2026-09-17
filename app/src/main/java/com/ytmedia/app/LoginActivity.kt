package com.ytmedia.app

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple built-in browser used only to sign in to the Google account.
 * When the session cookies appear, they are extracted and handed back
 * to the main app (shared CookieManager + persisted copy).
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var web: YtWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        root.setBackgroundColor(0xFF0F0F0F.toInt())
        web = YtWebView(this)
        // Desktop-ish UA avoids Google's "browser not secure" block on WebView sign-in.
        web.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36 YTMedia/1.0"
        root.addView(
            web,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setContentView(root)

        web.webViewClient = object : YtWebClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                checkLoggedIn(url)
            }
        }
        web.loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https://m.youtube.com/")
    }

    private fun checkLoggedIn(url: String?) {
        val cm = CookieManager.getInstance()
        val cookies = cm.getCookie("https://www.youtube.com") ?: return
        val hasSession = cookies.contains("SAPISID") || cookies.contains("__Secure-3PAPISID")
        if (hasSession && (url?.contains("youtube.com") == true)) {
            cm.flush()
            Prefs.saveCookies(this, cookies)
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
