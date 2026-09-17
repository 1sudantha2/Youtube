package com.ytmedia.app

import android.app.Application
import android.webkit.WebView

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            val p = getProcessName()
            if (packageName != p) WebView.setDataDirectorySuffix(p ?: "yt")
        }
        AdBlocker.init(this)
    }
}
