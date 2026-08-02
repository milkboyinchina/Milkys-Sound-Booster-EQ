import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

val envMap = mutableMapOf<String, String>()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
  envFile.readLines().forEach { line ->
    val trimmed = line.trim()
    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
      val parts = trimmed.split("=", limit = 2)
      envMap[parts[0].trim()] = parts[1].trim()
    }
  }
}

fun getEnvVar(key: String, defaultValue: String): String {
  return envMap[key] ?: System.getenv(key) ?: defaultValue
}

val envAppName = getEnvVar("APP_NAME", "Milkys Sound Booster & EQ")
val envApplicationId = getEnvVar("APPLICATION_ID", "com.milkys.soundbooster")
val envVersionCode = (getEnvVar("VERSION_CODE", "").toIntOrNull()
  ?: error("VERSION_CODE is missing or invalid in .env — please set a valid integer version code."))
val envVersionName = getEnvVar("VERSION_NAME", "").ifEmpty {
  error("VERSION_NAME is missing in .env — please set a version name (e.g. 0.1.1).")
}
val envBuildOutputDir = getEnvVar("BUILD_OUTPUT_DIR", ".build-outputs")
val envBuildLogsDir = getEnvVar("BUILD_LOGS_DIR", "logs")
val envScreenshotOutputDir = getEnvVar("SCREENSHOT_OUTPUT_DIR", "screenshots")
val envBuildTarget = getEnvVar("BUILD_TARGET", "playstore").lowercase()
val rawIncludeAds = getEnvVar("INCLUDE_GOOGLE_ADS", "")
val envIncludeGoogleAds = if (rawIncludeAds.isNotEmpty()) {
  rawIncludeAds.lowercase() == "true"
} else {
  envBuildTarget != "fdroid"
}
val envGithubRepoUrl = getEnvVar("GITHUB_REPO_URL", "https://github.com/milkys/sound-booster-eq")
val envDevWebsiteUrl = getEnvVar("DEVELOPER_WEBSITE_URL", "https://milkys.app")
val envPrivacyPolicyUrl = getEnvVar("PRIVACY_POLICY_URL", "https://milkys.app/privacy")

val envAppLogoDir = getEnvVar("APP_LOGO_DIR", "assets/logo")
val envAppLogoPath = getEnvVar("APP_LOGO_PATH", "assets/logo/app_logo.png")
val envAppLogoMdpi = getEnvVar("APP_LOGO_MDPI_PATH", "assets/logo/mdpi/app_logo.png")
val envAppLogoHdpi = getEnvVar("APP_LOGO_HDPI_PATH", "assets/logo/hdpi/app_logo.png")
val envAppLogoXhdpi = getEnvVar("APP_LOGO_XHDPI_PATH", "assets/logo/xhdpi/app_logo.png")
val envAppLogoXxhdpi = getEnvVar("APP_LOGO_XXHDPI_PATH", "assets/logo/xxhdpi/app_logo.png")
val envAppLogoXxxhdpi = getEnvVar("APP_LOGO_XXXHDPI_PATH", "assets/logo/xxxhdpi/app_logo.png")

val envAppIconDir = getEnvVar("APP_ICON_DIR", "assets/icon")
val envAppIconForegroundPath = getEnvVar("APP_ICON_FOREGROUND_PATH", "app/src/main/res/drawable/ic_launcher_foreground.xml")
val envAppIconBackgroundPath = getEnvVar("APP_ICON_BACKGROUND_PATH", "app/src/main/res/drawable/ic_launcher_background.xml")
val envAppIconMdpi = getEnvVar("APP_ICON_MDPI_PATH", "app/src/main/res/mipmap-mdpi/ic_launcher.png")
val envAppIconHdpi = getEnvVar("APP_ICON_HDPI_PATH", "app/src/main/res/mipmap-hdpi/ic_launcher.png")
val envAppIconXhdpi = getEnvVar("APP_ICON_XHDPI_PATH", "app/src/main/res/mipmap-xhdpi/ic_launcher.png")
val envAppIconXxhdpi = getEnvVar("APP_ICON_XXHDPI_PATH", "app/src/main/res/mipmap-xxhdpi/ic_launcher.png")
val envAppIconXxxhdpi = getEnvVar("APP_ICON_XXXHDPI_PATH", "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png")

android {
  namespace = "com.milkys.soundbooster"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = envApplicationId
    minSdk = getEnvVar("MIN_SDK", "24").toIntOrNull() ?: 24
    targetSdk = 36
    versionCode = envVersionCode
    versionName = envVersionName

    resValue("string", "app_name", envAppName)
    buildConfigField("String", "APP_NAME", "\"$envAppName\"")
    buildConfigField("String", "BUILD_TARGET", "\"$envBuildTarget\"")
    buildConfigField("Boolean", "INCLUDE_GOOGLE_ADS", envIncludeGoogleAds.toString())
    buildConfigField("String", "GITHUB_REPO_URL", "\"$envGithubRepoUrl\"")
    buildConfigField("String", "DEVELOPER_WEBSITE_URL", "\"$envDevWebsiteUrl\"")
    buildConfigField("String", "PRIVACY_POLICY_URL", "\"$envPrivacyPolicyUrl\"")
    buildConfigField("String", "SCREENSHOT_OUTPUT_DIR", "\"$envScreenshotOutputDir\"")

    buildConfigField("String", "APP_LOGO_DIR", "\"$envAppLogoDir\"")
    buildConfigField("String", "APP_LOGO_PATH", "\"$envAppLogoPath\"")
    buildConfigField("String", "APP_LOGO_MDPI_PATH", "\"$envAppLogoMdpi\"")
    buildConfigField("String", "APP_LOGO_HDPI_PATH", "\"$envAppLogoHdpi\"")
    buildConfigField("String", "APP_LOGO_XHDPI_PATH", "\"$envAppLogoXhdpi\"")
    buildConfigField("String", "APP_LOGO_XXHDPI_PATH", "\"$envAppLogoXxhdpi\"")
    buildConfigField("String", "APP_LOGO_XXXHDPI_PATH", "\"$envAppLogoXxxhdpi\"")

    buildConfigField("String", "APP_ICON_DIR", "\"$envAppIconDir\"")
    buildConfigField("String", "APP_ICON_FOREGROUND_PATH", "\"$envAppIconForegroundPath\"")
    buildConfigField("String", "APP_ICON_BACKGROUND_PATH", "\"$envAppIconBackgroundPath\"")
    buildConfigField("String", "APP_ICON_MDPI_PATH", "\"$envAppIconMdpi\"")
    buildConfigField("String", "APP_ICON_HDPI_PATH", "\"$envAppIconHdpi\"")
    buildConfigField("String", "APP_ICON_XHDPI_PATH", "\"$envAppIconXhdpi\"")
    buildConfigField("String", "APP_ICON_XXHDPI_PATH", "\"$envAppIconXxhdpi\"")
    buildConfigField("String", "APP_ICON_XXXHDPI_PATH", "\"$envAppIconXxxhdpi\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = getEnvVar("KEYSTORE_PATH", "${rootDir}/my-upload-key.jks")
      val keystoreFile = file(keystorePath)
      if (keystoreFile.exists()) {
        storeFile = keystoreFile
        storePassword = getEnvVar("STORE_PASSWORD", "android")
        keyAlias = getEnvVar("KEY_ALIAS", "upload")
        keyPassword = getEnvVar("KEY_PASSWORD", "android")
      } else {
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
      isCrunchPngs = true
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  packaging {
    resources {
      excludes += setOf(
        "META-INF/*.version",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE*",
        "META-INF/NOTICE*",
        "META-INF/licenses/*",
        "META-INF/AL2.0",
        "META-INF/LGPL2.1"
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
    resValues = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  // implementation(libs.converter.moshi)
  // implementation(platform(libs.firebase.bom))
  // implementation(libs.firebase.ai)
  // implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  if (envIncludeGoogleAds) {
    implementation(libs.play.services.ads)
  }
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
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register<Copy>("copyBuildOutputs") {
  group = "build"
  description = "Copies compiled APK outputs to the directory specified in BUILD_OUTPUT_DIR."
  from(layout.buildDirectory.dir("outputs/apk"))
  into(rootProject.file(envBuildOutputDir))
}

tasks.matching { it.name == "assembleDebug" || it.name == "assembleRelease" }.configureEach {
  finalizedBy("copyBuildOutputs")
}
