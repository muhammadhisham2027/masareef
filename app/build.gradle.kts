plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.muhammadhisham.masareef"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.muhammadhisham.masareef"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.1"
    }

    // Fixed signing key committed to the repo: every CI build produces the
    // same signature, so updates install cleanly over previous versions.
    // (Sideload-grade key for personal distribution — for a Play Store
    // launch, generate a private key and keep it out of the repo.)
    signingConfigs {
        create("shared") {
            storeFile = rootProject.file("masareef.keystore")
            storePassword = "masareef2026"
            keyAlias = "masareef"
            keyPassword = "masareef2026"
        }
    }

    buildTypes {
        getByName("debug") {
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

    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    // FragmentActivity needed for BiometricPrompt
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.biometric:biometric:1.1.0")
}
