package com.ytmedia.app

/** JavaScript injected into the YouTube web client. */
object Injector {

    /** Ad blocking + SponsorBlock segment skipping inside the real YouTube web player. */
    val AD_SKIP_JS = """
    (function(){
      if (window.__ytm_ads) return; window.__ytm_ads = true;

      var CSS = ['ytd-promoted-video-renderer','ytd-display-ad-renderer','ytd-ad-slot-renderer',
        'ytd-in-feed-ad-layout-renderer','ytm-promoted-video-renderer','ytm-companion-slot',
        'ad-slot-renderer','.ytp-ad-module','#masthead-ad','ytd-banner-promo-renderer',
        'ytd-statement-banner-renderer','.ytd-merch-shelf-renderer','ytm-promoted-sparkles-web-renderer',
        '.video-ads','.ytp-ad-overlay-container'];

      var st = document.createElement('style');
      st.textContent = CSS.join(',') + '{display:none !important;height:0 !important;}';
      (document.head||document.documentElement).appendChild(st);

      function killAds(){
        try{
          var v = document.querySelector('video');
          var p = document.querySelector('.html5-video-player');
          if (p && /ad-showing|ad-interrupting/.test(p.className) && v){
            if (isFinite(v.duration) && v.duration > 0) v.currentTime = v.duration;
            v.muted = true;
            var s = document.querySelector('.ytp-ad-skip-button,.ytp-ad-skip-button-modern,.ytp-skip-ad-button');
            if (s) s.click();
            var c = document.querySelector('.ytp-ad-overlay-close-button,.ytp-ad-overlay-close-container');
            if (c) c.click();
          }
          CSS.forEach(function(q){
            document.querySelectorAll(q).forEach(function(e){ e.remove(); });
          });
        }catch(e){}
      }

      /* ---------- SponsorBlock ---------- */
      var segs = [], loadedFor = null;
      function vid(){
        var m = location.href.match(/[?&]v=([\w-]{11})/);
        if (m) return m[1];
        m = location.pathname.match(/\/shorts\/([\w-]{11})/);
        return m ? m[1] : null;
      }
      function loadSegments(){
        var id = vid();
        if (!id || id === loadedFor) return;
        loadedFor = id; segs = [];
        fetch('https://sponsor.ajay.app/api/skipSegments?videoID=' + id +
              '&categories=' + encodeURIComponent(JSON.stringify(
              ['sponsor','selfpromo','interaction','intro','outro','music_offtopic'])))
          .then(function(r){ return r.ok ? r.json() : []; })
          .then(function(j){ segs = (j||[]).map(function(s){ return s.segment; }); })
          .catch(function(){});
      }
      function skipSponsor(){
        var v = document.querySelector('video');
        if (!v || !segs.length) return;
        var t = v.currentTime;
        for (var i=0;i<segs.length;i++){
          if (t > segs[i][0] && t < segs[i][1] - 0.2){ v.currentTime = segs[i][1]; break; }
        }
      }

      setInterval(function(){ killAds(); loadSegments(); skipSponsor(); }, 400);
      new MutationObserver(killAds).observe(document.documentElement,{childList:true,subtree:true});
    })();
    """.trimIndent()

    /** Reports playback state + current video to the native mini player. */
    val PLAYER_BRIDGE_JS = """
    (function(){
      if (window.__ytm_bridge) return; window.__ytm_bridge = true;
      function meta(){
        var v = document.querySelector('video');
        if (!v) return null;
        var t = document.querySelector('h1.ytd-watch-metadata, .slim-video-information-title, .title.ytd-video-primary-info-renderer');
        var m = location.href.match(/[?&]v=([\w-]{11})/) || location.pathname.match(/\/shorts\/([\w-]{11})/);
        return {
          id: m ? m[1] : '',
          title: t ? t.innerText.trim() : document.title.replace(' - YouTube',''),
          playing: !v.paused,
          time: v.currentTime || 0,
          duration: isFinite(v.duration) ? v.duration : 0
        };
      }
      setInterval(function(){
        try{
          var d = meta();
          if (d && window.YTM) YTM.onPlayerState(JSON.stringify(d));
          else if (window.YTM) YTM.onPlayerState('');
        }catch(e){}
      }, 700);
      window.__ytmToggle = function(){ var v=document.querySelector('video'); if(v){ v.paused ? v.play() : v.pause(); } };
      window.__ytmSeek = function(d){ var v=document.querySelector('video'); if(v){ v.currentTime += d; } };
    })();
    """.trimIndent()

    /** Makes the desktop/mobile web UI feel like the native app. */
    val UI_POLISH_JS = """
    (function(){
      var st = document.createElement('style');
      st.textContent = [
        'ytd-app, ytm-app { --yt-spec-base-background: #0f0f0f; }',
        '::-webkit-scrollbar{width:0;height:0}',
        'ytd-mealbar-promo-renderer, tp-yt-paper-dialog.ytd-popup-container[role="dialog"] .ytd-mealbar-promo-renderer {display:none!important}',
        'ytm-app { -webkit-tap-highlight-color: transparent; }',
        'body { overscroll-behavior-y: contain; }'
      ].join('\n');
      (document.head||document.documentElement).appendChild(st);
    })();
    """.trimIndent()
}
