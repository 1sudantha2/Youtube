# YT-MEDIA

Lightweight Android YouTube frontend client (`com.ytmedia.app`).

## Features
- **Real YouTube web UI** – uses the official YouTube web player, so playback/codecs always work.
- **Cached layout** – each tab (Home / Shorts / Subscriptions / Library) is a WebView kept alive in memory, so switching tabs never reloads.
- **Content from the YouTube APIs** – content is served by YouTube's own InnerTube endpoints via the web client.
- **Shorts support** – dedicated Shorts tab like the original app.
- **Ad + sponsor blocking** – network-level blocklist (`AdBlocker.kt`) plus in-page JS that skips video ads and SponsorBlock segments.
- **Native mini player** – collapse a video into a bottom mini bar (the thing the web site doesn't have), plus PiP.
- **Google sign-in** – a simple in-app browser (`LoginActivity`); cookies are extracted and shared with the main app so your account syncs (subscriptions, history, library).
- **Lightweight & smooth** – hardware layers, HTTP caching, memory trimming, no heavy dependencies.

## Build
GitHub Actions builds the APK on every push: **Actions → Build YT-MEDIA APK → Artifacts**
(`YT-MEDIA-debug` / `YT-MEDIA-release`).

Locally:
```
gradle assembleDebug
```
