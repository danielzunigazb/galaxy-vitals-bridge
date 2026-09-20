plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.danzuniga.vitalsbridge"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.danzuniga.vitalsbridge"
        minSdk = 29 // required by the Samsung Health Data SDK
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")

    // Samsung Health Data SDK (reads heart rate directly from the Samsung Health
    // app; Health Connect isn't usable here because Samsung Health never syncs
    // heart rate into it on this device, despite showing write permission as on).
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
    implementation("com.google.code.gson:gson:2.11.0")

    // Background sync
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Encrypted on-device token storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // HTTP client for the GitHub Contents API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
