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
        versionCode = 6
        versionName = "1.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
