plugins {
    id("com.android.application")
}

android {
    namespace = "in.ahilyanagardjs.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "in.ahilyanagardjs.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
