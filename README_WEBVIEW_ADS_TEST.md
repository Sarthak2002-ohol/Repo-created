# AhilyanagarDJ's Android v1.13 — WebView API for Ads test patch

Based on `Repo-created-main(3).zip`, root `Repo-created-main/` Android project.

## What changed

1. `app/build.gradle.kts`: version code 16, version 1.13; Google Mobile Ads
   Next-Gen SDK `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0`.
2. `app/src/main/java/in/ahilyanagardjs/app/MainActivity.java`: initialize
   the SDK on a background thread using the documented WebView-only application
   ID, register the existing main WebView on the UI thread before first load.
3. `.github/workflows/build-signed-release.yml`: rename build artifact to
   `AhilyanagarDJs-v1.13-SIGNED` / `AhilyanagarDJs-v1.13-release.apk`.

No change to `AndroidManifest.xml`, because this WebView-only Next-Gen API
uses `InitializationConfig.WEBVIEW_APIS_FOR_ADS_APPLICATION_ID` instead of an
AdMob app ID in the manifest. No native AdMob units were added.

## Install / build (private test first)

1. Back up your GitHub main branch or create a test branch.
2. Copy the three patch files into the matching paths of your existing
   Android GitHub project. Do not replace Firebase files, media services,
   icons, or signing credentials.
3. Commit the source changes and run your existing **Build Signed Release APK**
   GitHub Actions workflow. Your existing `ANDROID_KEYSTORE_B64` and
   `ANDROID_SIGNING_PASSWORD` secrets are required; never share them here.
4. Verify the workflow downloads/compiles dependencies, builds the app,
   validates the APK signature and outputs the v1.13 signed artifact.
5. Install over the currently signed app on your own test device, without
   uninstalling or clearing app data. Check login, play/pause, next/previous,
   background playback, lock-screen controls, notifications and download links.
6. To validate the WebView ads connection, on a TEMPORARY test build only,
   navigate the existing WebView to Google's official test URL:
   https://google.github.io/webview-ads/test/#api-for-ads-tests
   The WebView API for Ads test should show a green successful connection.
   Restore your normal music-app URL before making a production APK.
7. Keep Admin > Ads Manager > Android WebView Ads OFF until the above test,
   a compliant consent setup (where required), and ad-placement review pass.
8. After the signed APK has been successfully privately tested, upload it to
   your download endpoint and only then update your app-version.php endpoint.

## Safety and limitations

- The existing listener website is unchanged; the PHP ad manager still owns
  whether AdSense ad units render in the APK.
- Registration does not certify copyright permissions, consent requirements,
  placement policy, or invalid-traffic compliance.
- No new automated ad requests, refreshing, interstitials or audio interruption.
- No local Android SDK/Gradle environment was available to compile/sign here.
  The GitHub Actions build and real-device validation are mandatory.
- If compilation fails, do not publish or toggle Android ads ON. Review the
  build logs before proceeding.

Official Google documentation (checked Nov 2026):
https://developers.google.com/admob/android/next-gen/browser/webview/api-for-ads
https://developers.google.com/admob/android/next-gen/quick-start
