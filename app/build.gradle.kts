plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI passes -PversionCode=<run number> so every build is newer than the last one.
val vCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
val vName = (findProperty("versionName") as String?) ?: "1.0.0-dev"

val screenshots = providers.gradleProperty("screenshots").isPresent

android {
    namespace = "com.abc.tvlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abc.tvlauncher"
        minSdk = 28
        targetSdk = 34
        versionCode = vCode
        versionName = vName
    }

    // One fixed key for every build => each new APK installs over the old one.
    // Defaults point at the committed key; CI can override with repository secrets.
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "../keystore/launcher.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "abc-launcher-key"
            keyAlias = System.getenv("KEY_ALIAS") ?: "launcher"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "abc-launcher-key"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }

    if (screenshots) sourceSets.getByName("test").java.srcDir("src/screenshotTest/java")

    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
            all { t ->
                // Optional knobs for the screenshot run (see docs/SCREENSHOTS in README)
                (findProperty("screenshotDir") as String?)?.let { t.systemProperty("screenshotDir", it) }
                (findProperty("robolectricRepo") as String?)?.let { t.systemProperty("robolectric.dependency.repo.url", it) }
            }
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    // Screenshot tests (render the real UI + every intro to PNG without a device).
    // Only wired in with:  ./gradlew testDebugUnitTest -Pscreenshots
    if (screenshots) {
        testImplementation("org.robolectric:robolectric:4.14.1")
        testImplementation("androidx.compose.ui:ui-test-junit4")
        testImplementation("androidx.test:core:1.6.1")
        debugImplementation("androidx.compose.ui:ui-test-manifest")
    }
}
