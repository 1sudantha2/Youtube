package com.ytmedia.app

import android.content.Context

object Prefs {
    private const val FILE = "ytmedia"
    fun get(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var Context.signedIn: Boolean
        get() = get(this).getBoolean("signed_in", false)
        set(v) { get(this).edit().putBoolean("signed_in", v).apply() }

    fun saveCookies(ctx: Context, cookies: String) =
        get(ctx).edit().putString("yt_cookies", cookies).putBoolean("signed_in", true).apply()

    fun cookies(ctx: Context): String = get(ctx).getString("yt_cookies", "") ?: ""
}
