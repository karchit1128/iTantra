plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.2.10-2.0.2"
    id("org.jetbrains.kotlin.android")
}

base {
    archivesName.set("TalkBit-v1.0.0")
}

android {
    namespace = "com.example.itantra"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.itantra"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    
    
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    
    // Room Database
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    
    // ONNX
    implementation(files("libs/sherpa-onnx.aar"))
    
    // Mapbox/OSMDroid for Offline Cartography
    implementation("org.osmdroid:osmdroid-android:6.1.18")
}

tasks.register<Exec>("validateAssets") {
    workingDir = rootProject.projectDir
    if (org.gradle.internal.os.OperatingSystem.current().isWindows()) {
        commandLine("powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "scripts/validate_assets.ps1")
    } else {
        commandLine("pwsh", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", "scripts/validate_assets.ps1")
    }
}

tasks.named("preBuild") {
    dependsOn("validateAssets")
}

