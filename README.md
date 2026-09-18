# AhilyanagarDJ's Android App

Android WebView wrapper for: https://ahilyanagardjs.in/

## App details
- App name: AhilyanagarDJ's
- Package: `in.ahilyanagardjs.app`
- Version: 1.0
- Minimum Android: Android 6.0 (API 23)
- Target SDK: Android 15 / API 35

## Included behavior
- Loads the AhilyanagarDJ's website inside the app.
- JavaScript, DOM storage, cookies, images and media playback are enabled.
- `ahilyanagardjs.in` links stay inside the app.
- WhatsApp, Instagram, Superprofile, mail, phone and other external links open with Android's matching app/browser.
- Android Back button navigates website history before exiting.
- A thin loading progress bar is shown while pages load.
- Download links are handed to the device browser/download handler.

## Build locally
Requires Android SDK 35 and Gradle 8.9.

```bash
gradle assembleDebug
```

APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## Build with GitHub Actions
1. Create a GitHub repository and upload this project.
2. Open the repository's **Actions** tab.
3. Run **Build Android APK**.
4. Download the `AhilyanagarDJs-APK` artifact from the completed run.

The included workflow also builds automatically when pushed to the `main` branch.
