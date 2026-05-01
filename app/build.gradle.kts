plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.dragonbudget"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dragonbudget"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += "arm64-v8a"
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
            // LiteRT-LM 0.11+ bundles its own copies of the LiteRT + Qualcomm
            // dispatch native libs, which collide with the older standalone
            // litert and qnn-litert-delegate AARs. Prefer the first match —
            // project jniLibs (our patched libLiteRtDispatch_Qualcomm.so) win,
            // and otherwise the LiteRT-LM-bundled copies are used.
            pickFirsts += setOf(
                "**/libLiteRt.so",
                "**/libLiteRtClGlAccelerator.so",
                "**/libLiteRtGpuAccelerator.so",
                "**/libLiteRtOpenClAccelerator.so",
                "**/libLiteRtWebGpuAccelerator.so",
                "**/libLiteRtTopKOpenClSampler.so",
                "**/libLiteRtTopKWebGpuSampler.so",
                "**/libLiteRtDispatch_Qualcomm.so",
                "**/libGemmaModelConstraintProvider.so",
                "**/libQnnHtp.so",
                "**/libQnnHtpPrepare.so",
                "**/libQnnHtpV79Skel.so",
                "**/libQnnHtpV79Stub.so",
                "**/libQnnSystem.so",
                "**/libQnnTFLiteDelegate.so",
                "**/libqnn_delegate_jni.so",
                "**/liblitertlm_jni.so"
            )
        }
    }
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Navigation
    implementation(libs.navigation.compose)

    // CameraX
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    
    // ML Kit
    implementation(libs.mlkit.text.recognition)
    implementation(libs.kotlinx.coroutines.play.services)

    // LiteRT-LM
    implementation(libs.litertlm.android)

    // Standard LiteRT for Embeddings
    implementation(libs.litert.core)

    implementation("com.qualcomm.qti:qnn-litert-delegate:2.44.0")

    // Coroutines + Lifecycle
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}