plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.inkmode"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.inkmode"
        // Android 15 (API 35) is the first version that lets an app's Mode turn on grayscale.
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    // A fixed key so each new build installs as an update instead of needing an uninstall.
    signingConfigs {
        create("shared") {
            storeFile = file("inkmode.keystore")
            storePassword = "inkmode"
            keyAlias = "inkmode"
            keyPassword = "inkmode"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
