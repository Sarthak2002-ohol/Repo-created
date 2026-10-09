# AhilyanagarDJ's Android App v1.9

Android WebView wrapper for `https://ahilyanagardjs.in/music-app/`.

## App details
- Package: `in.ahilyanagardjs.app`
- Version: 1.9
- Version code: 11
- Minimum Android: Android 6.0 (API 23)
- Target SDK: Android 15 / API 35

## v1.9 upload fix
- Supports HTML file inputs inside the Android WebView.
- Tapping Profile Photo / Choose File can open Android Files/Photos.
- Uses Android's system picker; no broad storage permission is requested.
- The chosen file is returned to the webpage and normal website validation/upload continues.

## Preserved behavior
- Main page remains `https://ahilyanagardjs.in/music-app/`.
- JavaScript, DOM storage, cookies, images and media playback remain enabled.
- Internal AhilyanagarDJ's links stay inside the app according to the existing routing rules.
- External services continue to open through Android as configured.
- Existing download/save picker behavior remains unchanged.
- Existing analytics/update checks remain unchanged.
- Existing Back/Exit behavior remains unchanged.
- Custom pull-to-refresh remains disabled.
- The old top loading line remains hidden.

## Build signed release
This project includes `.github/workflows/build-signed-release.yml`. Keep the existing GitHub secrets from the v1.8 signed build:
- `ANDROID_KEYSTORE_B64`
- `ANDROID_SIGNING_PASSWORD`

Run **Build Signed Release APK** from GitHub Actions.

Expected artifact: `AhilyanagarDJs-v1.9-SIGNED`
Expected APK: `AhilyanagarDJs-v1.9-release.apk`

Use the same release key as v1.8. No signing key or password is included in this source package.
