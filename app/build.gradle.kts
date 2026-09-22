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
        versionCode = 9
        versionName = "1.7"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
}
