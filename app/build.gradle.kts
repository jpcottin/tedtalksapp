import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.screenshot)
  alias(libs.plugins.baselineprofile)
}

// Release signing is read from keystore.properties (local, gitignored) or, when
// absent, from environment variables (CI). Each value falls back local -> env so
// the same config serves both. If no keystore is configured the release build
// falls back to debug signing, keeping CI/contributor builds working without secrets.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
fun signingValue(propKey: String, envKey: String): String? =
    keystoreProperties.getProperty(propKey) ?: System.getenv(envKey)

android {
    namespace = "com.jpcexample.tedtalks"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.jpcexample.tedtalks"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("storeFile", "RELEASE_STORE_FILE")
            if (storeFilePath != null) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = signingValue("storePassword", "RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Use the real release key when configured; otherwise fall back to the
            // debug key so the minified build is still installable for verification
            // and CI/contributor builds don't need the signing secrets.
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                logger.warn(
                    "No release keystore configured (keystore.properties / RELEASE_* env). " +
                        "Falling back to debug signing for the release build."
                )
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    lint {
        disable += "Instantiatable"
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        // Compose Styles API (androidx.compose.foundation.style) and the MediaQuery
        // API (androidx.compose.ui.mediaQuery / UiMediaScope) are still experimental.
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.foundation.style.ExperimentalFoundationStyleApi",
            "-opt-in=androidx.compose.ui.ExperimentalMediaQueryApi",
            // ComposeUiFlags, used to switch MediaQuery integration on.
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi",
        )
    }
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)

  // Adaptive layout (Navigation 3 SceneStrategy)
  implementation(libs.androidx.compose.material3.adaptive)
  implementation(libs.androidx.compose.material3.adaptive.layout)
  implementation(libs.androidx.compose.material3.adaptive.navigation3)

  // Media3 ExoPlayer
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)

  // Icons
  implementation(libs.androidx.compose.material.icons.extended)

  // Image loading
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  // HTTP
  implementation(libs.okhttp)

  // Baseline profile installer (bundles + installs the generated profile at runtime)
  implementation(libs.androidx.profileinstaller)
  baselineProfile(project(":baselineprofile"))

  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.kxml2)
  testImplementation(libs.mockk)
  testImplementation(libs.okhttp.mockwebserver)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Compose Preview Screenshot Tests
  screenshotTestImplementation(libs.androidx.compose.ui.tooling)
  screenshotTestImplementation(libs.screenshot.validation.api)
}
