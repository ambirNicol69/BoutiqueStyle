import java.util.Properties
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val localProperties = Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use {
    localProperties.load(it)
}

android {
    namespace = "com.sanvalero.boutiquestyle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sanvalero.boutiquestyle"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val keyPath = localProperties.getProperty("storeFile")
            if (!keyPath.isNullOrBlank()) {
                storeFile = rootProject.file(keyPath)
                storePassword = localProperties.getProperty("storePassword")
                keyAlias = localProperties.getProperty("keyAlias")
                keyPassword = localProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}

val isolatedBuildDirectory = File(
    System.getenv("LOCALAPPDATA") ?: System.getProperty("java.io.tmpdir"),
    "BoutiqueStyleBuild/${rootProject.projectDir.name}/app"
)
layout.buildDirectory.set(isolatedBuildDirectory)

kapt { correctErrorTypes = true }

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    kapt(libs.androidx.room.compiler)
    kapt(libs.hilt.compiler)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

val syncDebugApk = tasks.register("syncDebugApk") {
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
        check(apk.isFile) { "Debug APK was not produced: $apk" }
        val destination = rootProject.file("app/build/outputs/apk/debug/app-debug.apk")
        destination.parentFile.mkdirs()
        apk.copyTo(destination, overwrite = true)
    }
}
tasks.configureEach {
    if (name == "assembleDebug") finalizedBy(syncDebugApk)
}

val syncReleaseApk = tasks.register("syncReleaseApk") {
    doLast {
        val apk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        check(apk.isFile) { "Release APK was not produced: $apk" }
        val destination = rootProject.file("app/build/outputs/apk/release/app-release.apk")
        destination.parentFile.mkdirs()
        apk.copyTo(destination, overwrite = true)
    }
}
tasks.configureEach {
    if (name == "assembleRelease") finalizedBy(syncReleaseApk)
}
