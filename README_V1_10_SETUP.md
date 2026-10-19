# AhilyanagarDJ's Android v1.10 — Push + Lock-screen Music

Package name: `in.ahilyanagardjs.app`
Version: `1.10`, versionCode `12` (the supplied v1.9 had versionCode `11`).

## What this source contains

- Android MediaSession-backed music controls with track cover, title, artist, Play/Pause, Previous, Next.
- Native playback notification in lock screen / Android Quick Settings, driven by the **existing** /music-app website's single HTML audio player.
- Android mediaPlayback foreground service required by Android 14+.
- Firebase Cloud Messaging notification service and `all_users` topic subscription.
- Runtime notification permission request (Android 13+) and separately configured Android channels.
- Existing v1.9 user interface, icon, and signing ID preserved.
- Signed-release GitHub workflow updated to name v1.10 and require Firebase JSON secret.

## Before building the signed APK

1. In Firebase Console > Project > Add Android app, use **exact package name**: `in.ahilyanagardjs.app`.
2. Your Firebase `google-services.json` is already included at `app/google-services.json` and matches the application ID. Do not register a new package.
3. GitHub Actions will use the included config directly. You do not need to create `FIREBASE_GOOGLE_SERVICES_JSON_B64`. Optionally, a secret with that name can override the included config. Never upload Firebase *service-account* credentials or private admin keys. Do NOT send keystores or release signing passwords to chat.
4. Preserve your existing GitHub Actions secrets `ANDROID_KEYSTORE_B64` and `ANDROID_SIGNING_PASSWORD`. An APK signed with a *different* signing key cannot update existing installed versions in place.
5. Replace the GitHub project source with this v1.10 source **while preserving your repo secrets**. Do not upload the obsolete duplicate v1.8 sub-project as the build root; Gradle uses top-level `app/`.
6. GitHub Actions > Build Signed Release APK > Run workflow. Expect `AhilyanagarDJs-v1.10-SIGNED` download.
7. Install over v1.9 on a test phone and test background playback, locksreen controls and FCM before sharing to users.
8. After validation upload new APK to your official URL and update `app-version.php` to latest_version_code 12 / latest_version_name 1.10 (preserve server's existing JSON contract).

## Firebase verification

The uploaded Firebase configuration matches Android package `in.ahilyanagardjs.app` and Firebase project `ahilyanagardjs-a54ee`. This is an ordinary mobile client config, not a Firebase Admin SDK private key. It is included so the ZIP can be uploaded to GitHub as-is.

## Send a Firebase test message

Firebase Console > Messaging > Create campaign > Notification. Target an Android installation or subscribed topic `all_users`. Allow notifications on Android 13+. Test title/body first, then test an optional `target_url` data field pointing to an `https://ahilyanagardjs.in/music-app/...` page.

## Playback architecture and limitations

- No duplicate native audio player: website `#audio` remains the only sound source; `assets/adc-native-media.js` is injected by the Android WebView into trusted pages.
- Existing `window.ADCPlayer.current()` supplies track title, artist and cover; website `#next` / `#previous` and HTMLAudioElement supply transport controls.
- Notification artwork is fetched with an HTTPS image size cap, and restricted to `ahilyanagardjs.in` so arbitrary web pages cannot ask Android to fetch remote artwork.
- Track artwork will only display if the website supplies an accessible cover image URL. Media controls are rendered by Android, and actual lockscreen visuals vary across Android versions.
- The website WebView must stay alive for background audio. If Android kills the app process or the user forcibly stops the app, playback cannot continue; true independent playback across process death would need a more invasive native Media3 streaming engine.
- Installing APK v1.10 once is necessary for users of v1.9 to receive Firebase pushes or Android media controls.
- This ZIP is SOURCE CODE, **not** a built or device-tested APK. The sandbox has no Android SDK / Gradle builder. Always test a signed build on a device first.
