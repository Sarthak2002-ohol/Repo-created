(function () {
  'use strict';
  // Runs ONLY inside the trusted top-level /music-app/ page of the Android WebView.
  if (window !== window.top || location.origin !== 'https://ahilyanagardjs.in' ||
      !location.pathname.startsWith('/music-app/')) return;
  if (!window.AndroidNativeMedia || window.__adcNativeMediaInstalled) return;
  window.__adcNativeMediaInstalled = true;
  var audio, last = '', lastReport = 0;
  function bridgeReport(force) {
    try {
      audio = document.getElementById('audio');
      if (!audio || !window.ADCPlayer || typeof window.ADCPlayer.current !== 'function') return;
      var t = window.ADCPlayer.current();
      var payload = t ? {
        title: String(t.title || ''), artist: String(t.artist || ''),
        cover: new URL(t.cover || '/music-app/assets/brand-logo.png', location.href).href,
        playing: !audio.paused && !audio.ended,
        position: Math.max(0, Math.floor(Number(audio.currentTime || 0) * 1000)),
        duration: Number.isFinite(audio.duration) ? Math.floor(audio.duration * 1000) : 0,
        hasTrack: !!audio.getAttribute('src')
      } : {hasTrack:false};
      var key = [payload.hasTrack,payload.title,payload.artist,payload.cover,payload.playing].join('|');
      var now = Date.now();
      if (!force && key === last && (now-lastReport) < 8000) return;
      last = key; lastReport = now;
      window.AndroidNativeMedia.update('__ADC_NATIVE_MEDIA_TOKEN__', JSON.stringify(payload));
    } catch (_ignored) {}
  }
  window.ADCNativeMediaControl = function (cmd) {
    try {
      audio = document.getElementById('audio');
      if (!audio) return;
      if (cmd === 'play') { var p = audio.play(); if (p && p.catch) p.catch(function(){}); }
      else if (cmd === 'pause') audio.pause();
      else if (cmd === 'next') {var n=document.getElementById('next');if(n)n.click();}
      else if (cmd === 'previous') {var v=document.getElementById('previous');if(v)v.click();}
      else if (cmd === 'stop') {var c=document.getElementById('mini-close');if(c)c.click();else audio.pause();}
      setTimeout(function(){bridgeReport(true);},200);
    } catch (_ignored) {}
  };
  function bind() {
    audio = document.getElementById('audio');
    if (!audio || !window.ADCPlayer) return false;
    ['play','playing','pause','ended','loadedmetadata','emptied','error'].forEach(function (name) {
      audio.addEventListener(name, function(){bridgeReport(true);});
    });
    audio.addEventListener('timeupdate', function(){bridgeReport(false);});
    bridgeReport(true);
    return true;
  }
  if (!bind()) {
    var attempts = 0, timer = setInterval(function(){
      if (bind() || ++attempts >= 35) clearInterval(timer);
    }, 500);
  }
  document.addEventListener('visibilitychange',function(){bridgeReport(true);});
})();
