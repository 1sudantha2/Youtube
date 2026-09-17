package com.ytmedia.app

import android.content.Context
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

/**
 * Network level blocker: kills ad / tracking / sponsor-beacon endpoints
 * before they ever reach the YouTube web player.
 */
object AdBlocker {

    private val EMPTY = ByteArrayInputStream(ByteArray(0))

    private val blockedHosts = hashSetOf(
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "google-analytics.com", "googletagservices.com", "googletagmanager.com",
        "adservice.google.com", "pagead2.googlesyndication.com",
        "static.doubleclick.net", "stats.g.doubleclick.net",
        "ad.youtube.com", "ads.youtube.com", "youtube.com/api/stats/ads",
        "s0.2mdn.net", "innovid.com", "moatads.com", "scorecardresearch.com"
    )

    private val blockedPaths = listOf(
        "/pagead/", "/ptracking", "/api/stats/ads", "/api/stats/qoe",
        "/youtubei/v1/log_event", "/generate_204", "/csi_204",
        "/get_midroll_info", "/ad_companion", "/pcs/activeview"
    )

    fun init(ctx: Context) { /* reserved for remote list updates */ }

    fun shouldBlock(url: String): Boolean {
        val u = url.lowercase()
        val host = runCatching { android.net.Uri.parse(u).host ?: "" }.getOrDefault("")
        if (blockedHosts.any { host == it || host.endsWith(".$it") }) return true
        if (blockedPaths.any { u.contains(it) }) return true
        return false
    }

    fun blockedResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", EMPTY)
}
