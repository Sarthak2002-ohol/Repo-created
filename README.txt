AhilyanagarDJ's Android v1.8 UI Hotfix

Changes:
- Disabled the custom pull-down-to-refresh gesture, so a small downward swipe while scrolling no longer reloads the page.
- Removed the visible top loading/progress line.
- Keeps the existing bottom-gap fix.
- Keeps Android Back -> Exit Yes/No confirmation.
- Keeps App Analytics and update checking.
- Keeps Home URL: https://ahilyanagardjs.in/music-app/
- No other app behavior changed.

Replace only:
app/src/main/java/in/ahilyanagardjs/app/MainActivity.java

Then commit to main and let GitHub Actions build a new APK.
