import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// Optional release signing: copy keystore.properties.example → keystore.properties at repo root.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

android {
    namespace = "com.tomsphone"
    compileSdk = 35
    
    defaultConfig {
        // Must match the Android application ID in Play Console (create app / upload key there first).
        applicationId = "com.ashbashapps.tomsphone"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.0.2"
        // Support & suggestions API (Vercel). Rebuild app after changing.
        buildConfigField("String", "SUPPORT_API_BASE_URL", "\"https://toms-phone.vercel.app\"")
        // Play Billing in-app product IDs (must match Play Console one-time products).
        buildConfigField("String", "BILLING_PRODUCT_LIFETIME_STANDARD", "\"wandas_lifetime_standard\"")
        buildConfigField("String", "BILLING_PRODUCT_LIFETIME_EARLY", "\"wandas_lifetime_early_adopter\"")
        // Optional fallback if Remote Config is empty (e.g. reviewer offline). Leave default empty; set only for review builds if needed.
        buildConfigField("String", "PLAY_REVIEW_LICENSE_FALLBACK", "\"\"")
        buildConfigField("Boolean", "BILLING_DEBUG_ENTITLEMENT_BYPASS", "false")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Native crash symbolication in Play: applies to JNI built in this module.
        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: error("keystore.properties: missing keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: error("keystore.properties: missing keyPassword")
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: error("keystore.properties: missing storePassword")
                val storeFilePath = keystoreProperties.getProperty("storeFile")
                    ?: error("keystore.properties: missing storeFile (path relative to repo root)")
                val keystoreFile = rootProject.file(storeFilePath)
                if (!keystoreFile.exists()) {
                    error("keystore.properties storeFile not found: ${keystoreFile.absolutePath}")
                }
                storeFile = keystoreFile
            }
        }
    }
    
    buildTypes {
        debug {
            buildConfigField("Boolean", "BILLING_DEBUG_ENTITLEMENT_BYPASS", "true")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // Prebuilt .so from dependencies (e.g. CameraX) are otherwise stripped; Play then warns about missing native symbols.
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
    }
}

dependencies {
    // Feature modules
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-phone"))
    implementation(project(":feature:feature-contacts"))
    implementation(project(":feature:feature-carer"))
    implementation(project(":feature:feature-kiosk"))
    
    // Core modules
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-tts"))
    implementation(project(":core:core-config"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-telecom"))
    
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    
    // Navigation
    implementation(libs.navigation.compose)
    
    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    
    // Analytics module
    implementation(project(":core:core-analytics"))
    implementation(project(":core:core-billing"))
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    
    // Debug
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}

