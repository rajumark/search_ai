
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ============================================================================
// DYNAMIC VERSIONING
// ============================================================================
fun generateVersionCode(): Int {
    val ciTimestamp = System.getenv("CI_VERSION_CODE")
    return if (!ciTimestamp.isNullOrEmpty()) {
        ciTimestamp.toIntOrNull() ?: calculateVersionCode()
    } else {
        calculateVersionCode()
    }
}

fun calculateVersionCode(): Int {
    val epoch = LocalDateTime.of(2020, 1, 1, 0, 0)
    val now = LocalDateTime.now()
    return ChronoUnit.MINUTES.between(epoch, now).toInt()
}

fun generateVersionName(): String {
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
        minSdk = 26
        targetSdk = 35
        
        versionCode = generateVersionCode()
        versionName = generateVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

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
            isMinifyEnabled = true
            isShrinkResources = true
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            val keystoreFilePath = System.getenv("KEYSTORE_FILE_PATH")
            signingConfig = if (!keystoreFilePath.isNullOrEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


    
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        xmlReport = true
        htmlReport = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}


dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.serialization.json)
    
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
    implementation(libs.androidx.lifecycle.runtime.compose)
    
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
    
    // ML Kit
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.face.detection)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.paging)
    
    // Coil
    implementation(libs.coil.compose)
    
    // Core Modules
    implementation(project(":core:database"))
    implementation(project(":core:work"))
    implementation(project(":core:data"))
    implementation(project(":core:permissions"))
    
    // Feature Modules
    implementation(project(":feature:ocr"))
    implementation(project(":feature:home"))
    implementation(project(":feature:labeling"))
    implementation(project(":feature:media"))
    implementation(project(":feature:media_processing"))
    
    // Domain
    implementation(project(":domain"))

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}