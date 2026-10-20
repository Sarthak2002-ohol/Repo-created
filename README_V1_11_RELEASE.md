# AhilyanagarDJ's — Android v1.11 Public Release

Package: `in.ahilyanagardjs.app`  
Version name: `1.11`  
Version code: `14`  
Minimum Android version: 7.0 (API 24)  
Firebase project: `ahilyanagardjs-a54ee`

## Changes from the v1.10.1 private test APK

- Remove the temporary popup that retrieves/copies the FCM registration token.
- Remove test-only clipboard code and extra imports.
- Keep existing Firebase Cloud Messaging and `all_users` topic subscription.
- Keep existing Android native lock-screen / notification media controls, track artwork, progress, headset buttons, and background playback via the original WebView audio player.
- Preserve package name, app icon, WebView home, navigation, update checker, and signing configuration.
- Raise version from `1.10.1` (13) to `1.11` (14); the signing certificate MUST match the installed version for an in-place upgrade.
- Name the signed GitHub Actions artifact `AhilyanagarDJs-v1.11-SIGNED`.

## Install source in GitHub (patch ZIP)

1. Extract `AhilyanagarDJs_v1.11_Public_Release_GitHub_Patch.zip`.
2. Open the existing `Sarthak2002-ohol/Repo-created` repository on the **main** branch.
3. Upload the extracted `app/` and `.github/` folders, preserving their directory paths and replacing corresponding files; do not upload the ZIP itself or create nested folders.
4. Commit the changes to `main`.
5. Verify `app/build.gradle.kts` has `versionCode = 14`, `versionName = "1.11"`, `minSdk = 24`.
6. Verify `app/google-services.json`, `AdcFirebaseService.java`, and `AdcMediaService.java` are still present from the working v1.10.1 repository.
7. In GitHub **Actions > Build Signed Release APK > Run workflow**, start the signed-release workflow. Ensure GitHub secrets `ANDROID_KEYSTORE_B64` and `ANDROID_SIGNING_PASSWORD` remain configured with the original release keystore. Never send or upload the private keystore to chat.
8. Wait for the full workflow to finish successfully including **Verify APK signature**, then download the `AhilyanagarDJs-v1.11-SIGNED` artifact, unzip, and install the APK OVER v1.10.1 (do not uninstall the old app).

## Test on your phone BEFORE public release

- Opens with no FCM token test popup; music app UI/layout unchanged.
- Previous login/data retained on in-place update.
- Background playback continues when screen is locked.
- Lock-screen and notification media controls show artwork and correct metadata; play/pause/previous/next work.
- Existing Android app can receive a private Firebase test message (do not publish a broad notification for testing).
- Notification tap brings you back to the app.
- App update-checker still works (server `app-version.php` is not modified by this patch).

**After all tests pass**, upload the signed APK to your production download endpoint, then update your existing `app-version.php` response to the new URL/version code `14`/version name `1.11`, respecting the server's current response structure. Do not change the update endpoint before the APK is available.

## Build limitations

This ZIP is Android SOURCE CODE, not a signed installable APK. Static source/archive tests were performed, but compilation and device testing must occur in GitHub Actions and on a real Android phone. No keystore is included; your original GitHub release secrets sign the APK.

The Google Firebase `google-services.json` file is an Android client configuration, not a service-account credential. Never upload Firebase Admin SDK JSON or a service-account private key in this source tree.
