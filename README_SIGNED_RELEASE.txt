AhilyanagarDJ's Signed Release APK Setup

This package configures the existing Android project to build a permanently signed release APK.

FILES TO REPLACE/ADD IN GITHUB
1. Replace:
   app/build.gradle.kts

2. Add:
   .github/workflows/build-signed-release.yml

GITHUB SECRETS REQUIRED
Open repository -> Settings -> Secrets and variables -> Actions -> New repository secret

Create:
- ANDROID_KEYSTORE_B64
- ANDROID_SIGNING_PASSWORD

Use the values from the separate PRIVATE signing backup package.
Never commit the .jks file, the Base64 value, or the password into the public repository.

BUILD
After the two files are in GitHub and the two secrets are saved:
Actions -> Build Signed Release APK -> Run workflow

Download artifact:
AhilyanagarDJs-v1.9-SIGNED

Inside:
AhilyanagarDJs-v1.9-release.apk

IMPORTANT
- Keep the same signing key forever for all future Android updates.
- Package name remains: in.ahilyanagardjs.app
- v1.9 uses versionCode 11 and updates the existing signed v1.8 build when the same signing key is used.
- The currently installed debug APK may have a different signature. If Android refuses to install the signed release over the debug build, uninstall the debug build first for this one-time transition. After users install the signed public release, future signed releases using the same key can update normally.


V1.9 FILE UPLOAD FIX
- Android WebView now supports HTML <input type="file"> via WebChromeClient.onShowFileChooser.
- Profile-photo, screenshot and other supported website upload fields can open Android Files/Photos.
- No storage permission is requested; Android's system document picker grants access only to the file the user selects.
- Existing download picker behavior remains separate and unchanged.
