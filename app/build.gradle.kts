plugins {
    id("com.android.application")
}

val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseSigningPassword = System.getenv("ANDROID_SIGNING_PASSWORD")

android {
    namespace = "in.ahilyanagardjs.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "in.ahilyanagardjs.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 16
        versionName = "1.13"
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (!releaseKeystorePath.isNullOrBlank() &&
            !releaseSigningPassword.isNullOrBlank()) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseSigningPassword
                keyAlias = "ahilyanagardjs_release"
                keyPassword = releaseSigningPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("release")?.let {
                signingConfig = it
            }
        }
    }
}

// Firebase config is supplied by the user, never replaced with a fake one.
// A debug build without the file can run the media controls, but cannot receive pushes.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

dependencies {
    // WebView API for Ads: links the existing AdSense web pages with Google's
    // supported Android WebView integration; no native ad unit is created.
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.5.0")
    implementation(platform("com.google.firebase:firebase-bom:35.0.2"))
    implementation("com.google.firebase:firebase-messaging")
}
