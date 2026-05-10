plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.tomsphone.core.analytics"
    compileSdk = 35
    
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    
    // Firebase — exclude Privacy Sandbox ads artifacts pulled by measurement SDK
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics) {
        exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices")
        exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices-java")
    }
    implementation(libs.firebase.crashlytics) {
        exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices")
        exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices-java")
    }
    implementation(libs.firebase.config) {
        exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices")
        exclude(group = "androidx.privacysandbox.ads", module = "ads-adservices-java")
    }
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
