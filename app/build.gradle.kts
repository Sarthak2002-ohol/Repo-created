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
        minSdk = 23
        targetSdk = 35
        versionCode = 10
        versionName = "1.8"
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

dependencies {
}
