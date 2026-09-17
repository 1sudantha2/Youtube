package com.ytmedia.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.ytmedia.app.databinding.ActivityMainBinding
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    /** Cached tab WebViews – created once, kept in memory so the layout never reloads. */
    private val tabs = SparseArray<YtWebView>()
    private var currentTab = R.id.nav_home

    private var player: YtWebView? = null
    private var playerExpanded = false
    private var playerActive = false

    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null

    private val tabUrls = mapOf(
        R.id.nav_home to "https://m.youtube.com/",
        R.id.nav_shorts to "https://m.youtube.com/shorts",
        R.id.nav_subs to "https://m.youtube.com/feed/subscriptions",
        R.id.nav_library to "https://m.youtube.com/feed/library"
    )

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            CookieManager.getInstance().flush()
            // Re-sync every cached tab with the freshly signed-in account.
            for (i in 0 until tabs.size()) tabs.valueAt(i).reload()
            invalidateOptionsMenu()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.bottomNav.setOnItemSelectedListener { item ->
            showTab(item.itemId); true
        }
        b.btnAccount.setOnClickListener { loginLauncher.launch(Intent(this, LoginActivity::class.java)) }
        b.btnSearch.setOnClickListener { openUrlInTab("https://m.youtube.com/results?search_query=") }

        b.miniBar.setOnClickListener { expandPlayer() }
        b.miniPlay.setOnClickListener { player?.evaluateJavascript("window.__ytmToggle&&__ytmToggle()", null) }
        b.miniClose.setOnClickListener { closePlayer() }

        showTab(R.id.nav_home)

        intent?.data?.let { openWatch(it.toString()) }
    }

    // ---------------------------------------------------------------- tabs

    @SuppressLint("SetJavaScriptEnabled")
    private fun getTab(id: Int): YtWebView {
        tabs[id]?.let { return it }
        val w = YtWebView(this)
        w.webViewClient = object : YtWebClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                if (isWatchUrl(url)) { openWatch(url); return true }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        w.webChromeClient = chromeClient()
        w.loadUrl(tabUrls[id] ?: "https://m.youtube.com/")
        tabs.put(id, w)
        return w
    }

    private fun showTab(id: Int) {
        val w = getTab(id)
        if (w.parent == null) {
            b.webHost.addView(
                w, FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
        for (i in 0 until tabs.size()) {
            val v = tabs.valueAt(i)
            v.visibility = if (v === w) View.VISIBLE else View.GONE
            if (v !== w) v.onPause() else v.onResume()
        }
        currentTab = id
    }

    private fun openUrlInTab(url: String) = getTab(currentTab).loadUrl(url)

    private fun isWatchUrl(url: String) =
        url.contains("/watch?") || url.contains("/shorts/") || url.contains("youtu.be/")

    // -------------------------------------------------------------- player

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensurePlayer(): YtWebView {
        player?.let { return it }
        val w = YtWebView(this)
        w.webViewClient = YtWebClient()
        w.webChromeClient = chromeClient()
        w.addJavascriptInterface(Bridge(), "YTM")
        b.playerHost.addView(
            w, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        player = w
        return w
    }

    private fun openWatch(url: String) {
        val w = ensurePlayer()
        w.loadUrl(url)
        playerActive = true
        expandPlayer()
    }

    private fun expandPlayer() {
        ensurePlayer()
        playerExpanded = true
        b.playerSheet.visibility = View.VISIBLE
        b.miniBar.visibility = View.GONE
        b.playerHost.visibility = View.VISIBLE
        b.bottomNav.visibility = View.GONE
        b.topBar.visibility = View.GONE
    }

    /** Collapse to the native mini player – playback keeps going. */
    private fun minimizePlayer() {
        if (!playerActive) return
        playerExpanded = false
        b.playerSheet.visibility = View.GONE
        b.playerHost.visibility = View.GONE
        b.miniBar.visibility = View.VISIBLE
        b.bottomNav.visibility = View.VISIBLE
        b.topBar.visibility = View.VISIBLE
    }

    private fun closePlayer() {
        playerActive = false
        playerExpanded = false
        b.miniBar.visibility = View.GONE
        b.playerSheet.visibility = View.GONE
        b.bottomNav.visibility = View.VISIBLE
        b.topBar.visibility = View.VISIBLE
        player?.loadUrl("about:blank")
    }

    inner class Bridge {
        @JavascriptInterface
        fun onPlayerState(json: String) {
            if (json.isEmpty()) return
            runOnUiThread {
                runCatching {
                    val o = JSONObject(json)
                    b.miniTitle.text = o.optString("title")
                    b.miniPlay.setImageResource(
                        if (o.optBoolean("playing")) R.drawable.ic_pause else R.drawable.ic_play
                    )
                    val d = o.optDouble("duration", 0.0)
                    b.miniProgress.progress =
                        if (d > 0) ((o.optDouble("time") / d) * 100).toInt() else 0
                }
            }
        }
    }

    // --------------------------------------------------------- fullscreen

    private fun chromeClient() = object : WebChromeClient() {
        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            customView = view; customCallback = callback
            b.fullscreenHost.visibility = View.VISIBLE
            b.fullscreenHost.addView(view)
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        override fun onHideCustomView() {
            b.fullscreenHost.removeAllViews()
            b.fullscreenHost.visibility = View.GONE
            customCallback?.onCustomViewHidden()
            customView = null
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER
        }
    }

    // ------------------------------------------------------------- system

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        when {
            customView != null -> (player ?: getTab(currentTab)).webChromeClient?.onHideCustomView()
            playerExpanded -> {
                val p = player
                if (p != null && p.canGoBack()) p.goBack() else minimizePlayer()
            }
            getTab(currentTab).canGoBack() -> getTab(currentTab).goBack()
            currentTab != R.id.nav_home -> b.bottomNav.selectedItemId = R.id.nav_home
            else -> super.onBackPressed()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerExpanded && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { enterPictureInPictureMode() }
        }
    }

    override fun onPictureInPictureModeChanged(inPip: Boolean, config: Configuration) {
        super.onPictureInPictureModeChanged(inPip, config)
        b.bottomNav.visibility = if (inPip) View.GONE else if (playerExpanded) View.GONE else View.VISIBLE
        b.topBar.visibility = if (inPip || playerExpanded) View.GONE else View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            // Keep the active tab, drop the rest to stay lightweight.
            for (i in tabs.size() - 1 downTo 0) {
                val key = tabs.keyAt(i)
                if (key != currentTab) {
                    val v = tabs.valueAt(i)
                    (v.parent as? ViewGroup)?.removeView(v)
                    v.destroy()
                    tabs.remove(key)
                }
            }
        }
    }
}
