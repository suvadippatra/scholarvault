plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.scholarvault"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.scholarvault"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0.0"
    multiDexEnabled = true
    
    ndk {
      abiFilters.add("arm64-v8a")
    }
    androidResources.localeFilters += setOf("en")

    splits {
      abi {
        isEnable = false
        reset()
        include("arm64-v8a")
        isUniversalApk = false
      }
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      val releaseKeystore = file(keystorePath)
      if (releaseKeystore.exists() && !System.getenv("KEYSTORE_PASSWORD").isNullOrEmpty()) {
        storeFile = releaseKeystore
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
      } else {
        // Fallback for CI builds without signing secrets setup
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
      }
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  packaging {
    jniLibs {
      useLegacyPackaging = false
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation("androidx.core:core:1.13.1")
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.pagecurl)
  implementation(libs.pdfbox.android)
  // Media3 (ExoPlayer)
  implementation("androidx.media3:media3-exoplayer:1.5.1")
  implementation("androidx.media3:media3-ui:1.5.1")
  implementation("androidx.media3:media3-session:1.5.1")
  // Platform Utilities & Data Persistence
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.coil.compose)
  implementation(libs.coil.gif)
  // implementation("io.coil-kt:coil:2.7.0")
  implementation(libs.androidx.print)
  // Room persistence
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.biometric)
  implementation(libs.androidx.fragment.ktx)
  // Removed moshi, retrofit, okhttp, and firebase dependencies to minimize offline footprint
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  // CameraX
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.camera.core)
  implementation(libs.accompanist.permissions)

  implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
  implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
  implementation("androidx.graphics:graphics-core:1.0.0-rc01")

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.register("printReleaseApkSize") {
  doLast {
    val apkDir = file("build/outputs/apk/release")
    if (apkDir.exists()) {
      apkDir.listFiles()?.forEach { 
        if (it.name.endsWith(".apk")) {
            println("=== APK SIZE OF ${it.name} IS: ${it.length()} BYTES ===")
        }
      }
    } else {
      println("=== APK DIR NOT FOUND ===")
    }
  }
}


