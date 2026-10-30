import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.bitstream.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bitstream.tv"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            // Optional local signing: put keystore.properties in the project root
            // (git-ignored) with storeFile/storePassword/keyAlias/keyPassword.
            val ksProps = rootProject.file("keystore.properties")
            if (ksProps.exists()) {
                val props = Properties().apply { ksProps.inputStream().use { load(it) } }
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Falls back to debug signing when no keystore.properties is present
            // (e.g. CI builds) so the APK is still installable.
            signingConfig =
                if (rootProject.file("keystore.properties").exists()) signingConfigs.getByName("release")
                else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    // Deliberately tiny dependency set — no appcompat, no compose, no room, no DI.
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    implementation("io.coil-kt:coil:2.7.0")             // poster loading (shares OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0") // networking
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
