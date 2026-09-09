plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "dev.jpcottin.tedtalksapp.baselineprofile"
    compileSdk = 37

    defaultConfig {
        // Macrobenchmark / baseline profile generation requires API 28+.
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // This is the app the generator drives to capture the profile.
    targetProjectPath = ":app"

    // Run the instrumentation against itself so it can launch the target app.
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

kotlin {
    jvmToolchain(17)
}

// Generate against a connected device by default. Override the device the
// generator runs on via -Pandroid.testInstrumentationRunnerArguments... if needed.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
