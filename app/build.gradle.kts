import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.beenthere.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.beenthere.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // L'app non ha test strumentati; nessun runner dichiarato.
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true // serve per BuildConfig.DEBUG (debug della WebView)
    }

    // Il GeoJSON viene compresso nell'APK. C'era un noCompress qui, e aveva
    // senso finche' il file pesava 189 KB; ora che ne pesa 719 la compressione
    // ne fa risparmiare circa mezzo mega, e la decompressione all'avvio costa
    // qualche decina di millisecondi - invisibili accanto ai secondi di
    // tassellatura del globo. WebViewAssetLoader serve gli asset compressi
    // senza accorgersene.

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.webkit) // WebViewAssetLoader
}
