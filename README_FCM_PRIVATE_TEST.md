# AhilyanagarDJ's v1.10.1 — private FCM device-token test patch

This is a **temporary test-only patch**, NOT a finished public release. It keeps the
existing music player, lock-screen playback, WebView navigation, app icon and
Firebase notification service unchanged.

## ZIP contents / GitHub upload

Upload these three files to **the corresponding exact paths** in your existing
`Sarthak2002-ohol/Repo-created` repo, branch `main`, replacing the old versions:

- `app/src/main/java/in/ahilyanagardjs/app/MainActivity.java`
- `app/build.gradle.kts` (minSdk 24, versionCode 13, versionName 1.10.1)
- `.github/workflows/build-signed-release.yml` (names test APK correctly)

Do NOT upload the ZIP itself expecting GitHub to extract it.
Do NOT delete `app/google-services.json`, `gradle.properties`, existing assets,
Java service files, or your existing signing secrets.

## Build and install

1. Commit the patch on `main`.
2. GitHub > Actions > Build Signed Release APK > Run workflow.
3. After the workflow succeeds download artifact
   `AhilyanagarDJs-v1.10.1-FCM-TEST-SIGNED`, extract it, and install the APK
   **over** v1.10 (same app package and same existing signing key).
4. Start the updated app on your phone. After its normal splash screen, an
   Android-native **Private Firebase notification test** dialog appears.
5. Tap **Copy FCM token**. Wait for `Token copied!` (must have internet).
6. On your computer Firebase Console > Messaging > Compose notification >
   **Send test message** > **Test on device** > paste token > **Test**.
7. Keep the Android app in the background for the initial test; enable app
   notifications in Android system settings if prompted.

### Privacy and retry

The FCM token is a per-app-install delivery identifier. Don't post screenshots
or messages containing it. Android's clipboard holds it briefly; consider
copying other harmless text after finishing. The app does NOT send the token to
any server or website. This dialog appears at most once per Activity launch;
reopen the app to copy the token again if needed. It is intentionally NOT part
of the regular website user interface.

### After testing

REMOVE this temporary token-copy helper before publishing the next APK to all
users. Do not upload this test build to the public APK download page.

### Validation status

Patch files and source insertion points were checked locally. GitHub/Android
must still compile the APK and test on a phone. If the build fails send logs.
