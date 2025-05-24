plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id ("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.allocate_optimize_track"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.allocate_optimize_track"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.storage.kt)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlin.serialization)
    implementation(libs.mpandroidchart)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.material.v1110)
    implementation (libs.kotlinx.coroutines.android)

    // Lifecycle components for ViewModel and LiveData
    implementation (libs.androidx.lifecycle.viewmodel.ktx) // Or latest
    implementation (libs.androidx.lifecycle.livedata.ktx)// Or latest
    implementation (libs.androidx.lifecycle.runtime.ktx.v270)// Or latest

    implementation (libs.androidx.recyclerview)


    implementation (libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation (libs.androidx.fragment.ktx)
    implementation( libs.androidx.navigation.fragment)
    implementation (libs.androidx.navigation.ui)
    implementation(libs.glide)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.0.0")) // Check for latest BOM
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx") // Realtime Database

    // Supabase

    implementation("io.ktor:ktor-client-android:3.1.1")

    //Charts
    implementation("com.github.PhilJay:MPAndroidchart:v3.1.0")
}