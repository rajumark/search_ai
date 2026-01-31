import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// ============================================================================
// DYNAMIC VERSIONING
// Generates version code and name automatically based on datetime
// Format: YYYYMMDDHHMM (e.g., 202601311628)
// This ensures unique versions without manual bumping
// ============================================================================
fun generateVersionCode(): Int {
    // Use CI-provided timestamp or current time
    val ciTimestamp = System.getenv("CI_VERSION_CODE")
    return if (!ciTimestamp.isNullOrEmpty()) {
        ciTimestamp.toIntOrNull() ?: run {
            val now = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyMMddHHmm")
            now.format(formatter).toInt()
        }
    } else {
        // For local development: use shorter format to stay within Int range
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyMMddHHmm")
        now.format(formatter).toInt()
    }
}

fun generateVersionName(): String {
    // Use CI-provided version name or generate from date
    val ciVersionName = System.getenv("CI_VERSION_NAME")
    return if (!ciVersionName.isNullOrEmpty()) {
        ciVersionName
    } else {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd-HHmm")
        now.format(formatter)
    }
}

android {
    namespace = "com.photo.searchai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.photo.searchai"
        minSdk = 24
        targetSdk = 35
        
        // Dynamic versioning - no manual bumps required
        versionCode = generateVersionCode()
        versionName = generateVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Build universal APK (all ABIs in one)
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    // ============================================================================
    // SIGNING CONFIGURATION
    // Reads signing credentials from environment variables (CI/CD secrets)
    // Falls back gracefully if secrets are not available
    // ============================================================================
    signingConfigs {
        create("release") {
            val keystoreFilePath = System.getenv("KEYSTORE_FILE_PATH")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("KEY_ALIAS")
            val keyPasswordValue = System.getenv("KEY_PASSWORD")
            
            if (!keystoreFilePath.isNullOrEmpty() && 
                !keystorePassword.isNullOrEmpty() && 
                !keyAliasValue.isNullOrEmpty() && 
                !keyPasswordValue.isNullOrEmpty()) {
                storeFile = file(keystoreFilePath)
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            // ================================================================
            // R8/PROGUARD CONFIGURATION
            // Enable full minification and obfuscation for release builds
            // ================================================================
            isMinifyEnabled = true
            isShrinkResources = true
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Use release signing config if available
            val keystoreFilePath = System.getenv("KEYSTORE_FILE_PATH")
            if (!keystoreFilePath.isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.13"
    }
    
    // ============================================================================
    // LINT CONFIGURATION
    // Ensures lint checks are performed before release builds
    // ============================================================================
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        xmlReport = true
        htmlReport = true
    }
    
    // ============================================================================
    // PACKAGING OPTIONS
    // Handle duplicate files in dependencies
    // ============================================================================
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    
    // ML Kit Text Recognition
    implementation(libs.mlkit.text.recognition)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.paging)
    
    // Coil
    implementation(libs.coil.compose)
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}