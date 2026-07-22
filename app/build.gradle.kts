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

val envVersionCode = getEnvVar("VERSION_CODE", "3").toIntOrNull() ?: 3
val envVersionName = getEnvVar("VERSION_NAME", "3.0")
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
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.volumebooster.vmbstr"
    minSdk = 24
    targetSdk = 36
    versionCode = envVersionCode
    versionName = envVersionName

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
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
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
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
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
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Firebase Auth with Google Sign-In requires all of the following to be uncommented together.
  // If you are using Firebase Auth with other providers (e.g. Email/Password), you may only need
  // firebase-auth.
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.play.services.ads)
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
  "ksp"(libs.moshi.kotlin.codegen)
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
