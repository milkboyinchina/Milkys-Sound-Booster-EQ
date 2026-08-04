package com.milkys.soundbooster

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.ui.BiasAlignment
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.milkys.soundbooster.ui.theme.MyApplicationTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast

enum class WindowSizeGroup {
    COMPACT,  // < 600 dp (Phones)
    MEDIUM,   // 600 dp .. 839 dp (Small Tablets / Foldables)
    EXPANDED  // >= 840 dp (Large Tablets / Desktop)
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        AudioEffectManager.init(this)
        AudioEffectManager.applyLanguageToApp(this, AudioEffectManager.appLanguage.value)
        
        // Ensure WebView cache directories exist to handle Chromium/AdMob engine cache enumeration
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!webViewCacheDir.exists()) {
                webViewCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            // Non-critical cache directory creation fallback
        }

        // Start background service on launch if enabled
        if (AudioEffectManager.isBoostEnabled.value) {
            startBoosterService()
        }

        // Initialize Google Mobile Ads SDK via reflection if included in the build
        val isAdsIncluded = try {
            BuildConfig.INCLUDE_GOOGLE_ADS.toString().toBoolean()
        } catch (e: Exception) {
            false
        }
        if (isAdsIncluded) {
            try {
                val mobileAdsClass = Class.forName("com.google.android.gms.ads.MobileAds")
                val initMethod = mobileAdsClass.getMethod("initialize", Context::class.java)
                initMethod.invoke(null, this)
            } catch (e: Exception) {
                // Safe ignore for F-Droid builds without Google Ads SDK
            }
        }

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF1C1B1F) // Clean Minimalism background
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        onStartService = { startBoosterService() },
                        onStopService = { stopBoosterService() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AudioEffectManager.checkBatterySaverState()
    }

    private fun startBoosterService() {
        val intent = Intent(this, VolumeBoosterService::class.java).apply {
            action = VolumeBoosterService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun stopBoosterService() {
        val intent = Intent(this, VolumeBoosterService::class.java).apply {
            action = VolumeBoosterService.ACTION_STOP
        }
        try {
            startService(intent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = LocalContext.current
    val isEnabled by AudioEffectManager.isBoostEnabled.collectAsState()
    val boostProgress by AudioEffectManager.boostProgress.collectAsState()
    val currentPreset by AudioEffectManager.eqPreset.collectAsState()
    val defaultPreset by AudioEffectManager.defaultPreset.collectAsState()
    val customPresets by AudioEffectManager.customPresets.collectAsState()
    val eqBands by AudioEffectManager.eqBands.collectAsState()
    val isFloatingEnabled by AudioEffectManager.isFloatingEnabled.collectAsState()
    
    val isBatterySaverOn by AudioEffectManager.isBatterySaverOn.collectAsState()
    val isBatteryOptimized by AudioEffectManager.isBatteryOptimized.collectAsState()

    var showPermissionExplanation by remember { mutableStateOf(false) }

    // Request Notification Permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission status handled
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val check = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (check != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Checking overlay permission dynamically when app returns from background
    var hasOverlayPermission by remember { mutableStateOf(true) }
    LaunchedEffect(isFloatingEnabled) {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    val isAdsIncluded = remember {
        try {
            BuildConfig.INCLUDE_GOOGLE_ADS.toString().toBoolean()
        } catch (e: Exception) {
            true
        }
    }
    val isAdsEnabled by AudioEffectManager.isAdsEnabled.collectAsState()
    val isSliderStepped by AudioEffectManager.isSliderStepped.collectAsState()
    val isNotifControlsEnabled by AudioEffectManager.isNotifControlsEnabled.collectAsState()
    val hasSeenOnboarding by AudioEffectManager.hasSeenOnboarding.collectAsState()
    val isHearingWarningDisabled by AudioEffectManager.isHearingWarningDisabled.collectAsState()
    val hearingWarningHiddenUntil by AudioEffectManager.hearingWarningHiddenUntil.collectAsState()
    val appLanguage by AudioEffectManager.appLanguage.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showOnboardingManually by remember { mutableStateOf(false) }
    var isSoundTesting by remember { mutableStateOf(false) }
    var showPrivacyTermsDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }

    val isDarkTheme by AudioEffectManager.isDarkTheme.collectAsState()

    val bgColor = if (isDarkTheme) Color(0xFF1C1B1F) else Color(0xFFF6F2FA)
    val cardColor = if (isDarkTheme) Color(0xFF2B2930) else Color(0xFFFFFFFF)
    val textPrimary = if (isDarkTheme) Color(0xFFE6E1E5) else Color(0xFF1D1B20)
    val textSecondary = if (isDarkTheme) Color(0xFFCAC4D0) else Color(0xFF49454F)
    val primaryAccent = if (isDarkTheme) Color(0xFFD0BCFF) else Color(0xFF6750A4)
    val borderDivider = if (isDarkTheme) Color(0xFF49454F) else Color(0xFFE7E0EC)
    val dialBgColor = if (isDarkTheme) Color(0xFF1C1B1F) else Color(0xFFF3EDF7)

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    fun playSoundTest3Sec() {
        if (isSoundTesting) return
        isSoundTesting = true
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 44100
                val durationSeconds = 3
                val numSamples = sampleRate * durationSeconds
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = when {
                        t < 0.75 -> 523.25
                        t < 1.5 -> 659.25
                        t < 2.25 -> 783.99
                        else -> 1046.50
                    }
                    val secondFreq = if (t >= 2.25) 1318.51 else freq * 0.5
                    val wave = (Math.sin(2.0 * Math.PI * freq * t) + 0.5 * Math.sin(2.0 * Math.PI * secondFreq * t))
                    val envelope = if (t > 2.7) (3.0 - t) / 0.3 else 1.0
                    val sampleValue = (wave * 0.3 * envelope * Short.MAX_VALUE).toInt()
                    buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                delay(3000)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    isSoundTesting = false
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val windowSizeGroup = when {
        screenWidthDp < 600 -> WindowSizeGroup.COMPACT
        screenWidthDp < 840 -> WindowSizeGroup.MEDIUM
        else -> WindowSizeGroup.EXPANDED
    }

    DisposableEffect(windowSizeGroup) {
        val activity = context as? android.app.Activity
        if (activity != null) {
            when (windowSizeGroup) {
                WindowSizeGroup.COMPACT -> {
                    // Small size class (phones): disable landscape view (lock to portrait)
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                WindowSizeGroup.MEDIUM, WindowSizeGroup.EXPANDED -> {
                    // Medium & Expanded size classes (small tablets/foldables & large tablets): follow system screen orientation
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
        onDispose {}
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(10.dp)
    ) {
        when (windowSizeGroup) {
            WindowSizeGroup.COMPACT -> {
                // Compact (Phones): 1-Column Layout
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
                    ) {
                        // Top Header Row
                        item {
                            AppHeaderRow(
                                windowSizeGroup = windowSizeGroup,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { AudioEffectManager.setDarkTheme(!isDarkTheme) },
                                onOpenSettings = { showSettings = true },
                                onOpenOnboarding = { showOnboardingManually = true },
                                onSoundTest = { playSoundTest3Sec() },
                                isSoundTesting = isSoundTesting
                            )
                        }

                        // Hearing Loss Safety Warning Card (Moved to the Top)
                        if (!isHearingWarningDisabled && System.currentTimeMillis() > hearingWarningHiddenUntil) {
                            item {
                                HearingWarningCard(
                                    onClose = {
                                        AudioEffectManager.hideHearingWarningFor7Days()
                                        showHearingWarningDismissedNotification(context)
                                    }
                                )
                            }
                        }

                        // Native Ad card on top of Decibel Booster
                        item {
                            NativeAdCard(
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent
                            )
                        }

                        // Circular Dial & Boost Controls
                        item {
                            DecibelBoosterCard(
                                isEnabled = isEnabled,
                                boostProgress = boostProgress,
                                isSoundTesting = isSoundTesting,
                                isSliderStepped = isSliderStepped,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                dialBgColor = dialBgColor,
                                onSoundTest = { playSoundTest3Sec() },
                                onTogglePower = {
                                    val nextState = !isEnabled
                                    AudioEffectManager.setBoostEnabled(nextState)
                                    if (nextState) onStartService() else onStopService()
                                },
                                onBoostChange = { AudioEffectManager.setBoostProgress(it) }
                            )
                        }

                        // Quick Boost Presets Pill Row
                        item {
                            QuickBoostPresetsCard(
                                isEnabled = isEnabled,
                                boostProgress = boostProgress,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                onSelectPreset = { AudioEffectManager.setBoostProgress(it) }
                            )
                        }

                        // Adaptive Banner Ad on top of Equalizer
                        item {
                            AdaptiveBannerAdCard(
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent
                            )
                        }

                        // Visual Equalizer Section
                        item {
                            VisualEqualizerCard(
                                isEnabled = isEnabled,
                                eqBands = eqBands,
                                currentPreset = currentPreset,
                                defaultPreset = defaultPreset,
                                customPresets = customPresets,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                onBandChange = { band, level -> AudioEffectManager.setBandLevel(band, level) },
                                onApplyPreset = { preset -> AudioEffectManager.applyPreset(preset) },
                                onSaveCustomPreset = { name, bands -> AudioEffectManager.saveCustomPreset(name, bands) },
                                onDeleteCustomPreset = { name -> AudioEffectManager.deleteCustomPreset(name) },
                                onSetDefaultPreset = { name -> AudioEffectManager.setDefaultPreset(name) },
                                onExportPreset = { name -> AudioEffectManager.exportPreset(name) },
                                onExportAllPresets = { AudioEffectManager.exportAllPresets() },
                                onImportPreset = { json -> AudioEffectManager.importPreset(json) }
                            )
                        }

                        // System Optimization Diagnostics
                        item {
                            SystemBatteryDiagnosticCard(
                                isBatterySaverOn = isBatterySaverOn,
                                isBatteryOptimized = isBatteryOptimized,
                                context = context
                            )
                        }
                    }
                }
            }

            WindowSizeGroup.MEDIUM -> {
                // Medium (Small Tablets / Foldables): 2-Column Split Pane
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    AppHeaderRow(
                        windowSizeGroup = windowSizeGroup,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        primaryAccent = primaryAccent,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { AudioEffectManager.setDarkTheme(!isDarkTheme) },
                        onOpenSettings = { showSettings = true },
                        onOpenOnboarding = { showOnboardingManually = true },
                        onSoundTest = { playSoundTest3Sec() },
                        isSoundTesting = isSoundTesting
                    )

                    if (!isHearingWarningDisabled && System.currentTimeMillis() > hearingWarningHiddenUntil) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HearingWarningCard(
                            onClose = {
                                AudioEffectManager.hideHearingWarningFor7Days()
                                showHearingWarningDismissedNotification(context)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            NativeAdCard(
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent
                            )

                            DecibelBoosterCard(
                                isEnabled = isEnabled,
                                boostProgress = boostProgress,
                                isSoundTesting = isSoundTesting,
                                isSliderStepped = isSliderStepped,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                dialBgColor = dialBgColor,
                                onSoundTest = { playSoundTest3Sec() },
                                onTogglePower = {
                                    val nextState = !isEnabled
                                    AudioEffectManager.setBoostEnabled(nextState)
                                    if (nextState) onStartService() else onStopService()
                                },
                                onBoostChange = { AudioEffectManager.setBoostProgress(it) }
                            )

                            QuickBoostPresetsCard(
                                isEnabled = isEnabled,
                                boostProgress = boostProgress,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                onSelectPreset = { AudioEffectManager.setBoostProgress(it) }
                            )

                            SystemBatteryDiagnosticCard(
                                isBatterySaverOn = isBatterySaverOn,
                                isBatteryOptimized = isBatteryOptimized,
                                context = context
                            )
                        }

                        // Right Pane
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AdaptiveBannerAdCard(
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent
                            )

                            VisualEqualizerCard(
                                isEnabled = isEnabled,
                                eqBands = eqBands,
                                currentPreset = currentPreset,
                                defaultPreset = defaultPreset,
                                customPresets = customPresets,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                onBandChange = { band, level -> AudioEffectManager.setBandLevel(band, level) },
                                onApplyPreset = { preset -> AudioEffectManager.applyPreset(preset) },
                                onSaveCustomPreset = { name, bands -> AudioEffectManager.saveCustomPreset(name, bands) },
                                onDeleteCustomPreset = { name -> AudioEffectManager.deleteCustomPreset(name) },
                                onSetDefaultPreset = { name -> AudioEffectManager.setDefaultPreset(name) },
                                onExportPreset = { name -> AudioEffectManager.exportPreset(name) },
                                onExportAllPresets = { AudioEffectManager.exportAllPresets() },
                                onImportPreset = { json -> AudioEffectManager.importPreset(json) }
                            )
                        }
                    }
                }
            }

            WindowSizeGroup.EXPANDED -> {
                // Expanded (Large Tablets / Desktop): 3-Pane Dashboard Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    AppHeaderRow(
                        windowSizeGroup = windowSizeGroup,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        primaryAccent = primaryAccent,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { AudioEffectManager.setDarkTheme(!isDarkTheme) },
                        onOpenSettings = { showSettings = true },
                        onOpenOnboarding = { showOnboardingManually = true },
                        onSoundTest = { playSoundTest3Sec() },
                        isSoundTesting = isSoundTesting
                    )

                    if (!isHearingWarningDisabled && System.currentTimeMillis() > hearingWarningHiddenUntil) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HearingWarningCard(
                            onClose = {
                                AudioEffectManager.hideHearingWarningFor7Days()
                                showHearingWarningDismissedNotification(context)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Pane 1: Native Ad & Master Dial
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            NativeAdCard(
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent
                            )

                            DecibelBoosterCard(
                                isEnabled = isEnabled,
                                boostProgress = boostProgress,
                                isSoundTesting = isSoundTesting,
                                isSliderStepped = isSliderStepped,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                dialBgColor = dialBgColor,
                                onSoundTest = { playSoundTest3Sec() },
                                onTogglePower = {
                                    val nextState = !isEnabled
                                    AudioEffectManager.setBoostEnabled(nextState)
                                    if (nextState) onStartService() else onStopService()
                                },
                                onBoostChange = { AudioEffectManager.setBoostProgress(it) }
                            )
                        }

                        // Pane 2: Visual Equalizer & Quick Presets
                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AdaptiveBannerAdCard(
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent
                            )

                            VisualEqualizerCard(
                                isEnabled = isEnabled,
                                eqBands = eqBands,
                                currentPreset = currentPreset,
                                defaultPreset = defaultPreset,
                                customPresets = customPresets,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                onBandChange = { band, level -> AudioEffectManager.setBandLevel(band, level) },
                                onApplyPreset = { preset -> AudioEffectManager.applyPreset(preset) },
                                onSaveCustomPreset = { name, bands -> AudioEffectManager.saveCustomPreset(name, bands) },
                                onDeleteCustomPreset = { name -> AudioEffectManager.deleteCustomPreset(name) },
                                onSetDefaultPreset = { name -> AudioEffectManager.setDefaultPreset(name) },
                                onExportPreset = { name -> AudioEffectManager.exportPreset(name) },
                                onExportAllPresets = { AudioEffectManager.exportAllPresets() },
                                onImportPreset = { json -> AudioEffectManager.importPreset(json) }
                            )

                            QuickBoostPresetsCard(
                                isEnabled = isEnabled,
                                boostProgress = boostProgress,
                                cardColor = cardColor,
                                borderDivider = borderDivider,
                                textSecondary = textSecondary,
                                primaryAccent = primaryAccent,
                                onSelectPreset = { AudioEffectManager.setBoostProgress(it) }
                            )
                        }

                        // Pane 3: System Status
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SystemBatteryDiagnosticCard(
                                isBatterySaverOn = isBatterySaverOn,
                                isBatteryOptimized = isBatteryOptimized,
                                context = context
                            )
                        }
                    }
                }
            }
        }
    }

    // Google Ads & App Settings Dialog
    if (showSettings) {
        Dialog(
            onDismissRequest = { showSettings = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1C1B1F)),
                color = Color(0xFF1C1B1F)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SETTINGS",
                            color = Color(0xFFE6E1E5),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = { showSettings = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Settings",
                                tint = Color(0xFFE6E1E5)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Loudness & Notification Controls Section
                    Text(
                        text = stringResource(R.string.amplifier_notification_controls),
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 10% Stepped Slider Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.stepped_slider_title),
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isSliderStepped) stringResource(R.string.stepped_slider_desc_on) else stringResource(R.string.stepped_slider_desc_off),
                                        color = Color(0xFFCAC4D0),
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = isSliderStepped,
                                    onCheckedChange = { AudioEffectManager.setSliderStepped(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFD0BCFF),
                                        checkedTrackColor = Color(0xFF49454F),
                                        uncheckedThumbColor = Color(0xFFCAC4D0),
                                        uncheckedTrackColor = Color(0xFF49454F)
                                    ),
                                    modifier = Modifier.testTag("stepped_slider_toggle")
                                )
                            }

                            Divider(color = Color(0xFF49454F), thickness = 1.dp)

                            // Notification Bar Controls Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.notification_controls_title),
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.notification_controls_desc),
                                        color = Color(0xFFCAC4D0),
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = isNotifControlsEnabled,
                                    onCheckedChange = { AudioEffectManager.setNotifControlsEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFD0BCFF),
                                        checkedTrackColor = Color(0xFF49454F),
                                        uncheckedThumbColor = Color(0xFFCAC4D0),
                                        uncheckedTrackColor = Color(0xFF49454F)
                                    ),
                                    modifier = Modifier.testTag("notif_controls_toggle")
                                )
                            }

                            Divider(color = Color(0xFF49454F), thickness = 1.dp)

                            // Disable Hearing Protection Warning Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.disable_hearing_warning_title),
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.disable_hearing_warning_desc),
                                        color = Color(0xFFCAC4D0),
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = isHearingWarningDisabled,
                                    onCheckedChange = { AudioEffectManager.setHearingWarningDisabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFD0BCFF),
                                        checkedTrackColor = Color(0xFF49454F),
                                        uncheckedThumbColor = Color(0xFFCAC4D0),
                                        uncheckedTrackColor = Color(0xFF49454F)
                                    ),
                                    modifier = Modifier.testTag("disable_hearing_warning_toggle")
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Language Selection Section (Android Per-App Language Preferences)
                    Text(
                        text = stringResource(R.string.language_preference_section),
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.app_language_title),
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.app_language_desc),
                                        color = Color(0xFFCAC4D0),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Divider(color = Color(0xFF49454F), thickness = 1.dp)

                            var languageDropdownExpanded by remember { mutableStateOf(false) }

                            val languageOptions = listOf(
                                "system" to stringResource(R.string.system_default_language),
                                "en" to stringResource(R.string.language_english),
                                "id" to stringResource(R.string.language_indonesian),
                                "ms" to stringResource(R.string.language_malay),
                                "hi" to stringResource(R.string.language_hindi),
                                "pt" to stringResource(R.string.language_portuguese),
                                "fr" to stringResource(R.string.language_french),
                                "it" to stringResource(R.string.language_italian),
                                "de" to stringResource(R.string.language_german),
                                "zh-CN" to stringResource(R.string.language_simplified_chinese),
                                "zh-TW" to stringResource(R.string.language_traditional_chinese),
                                "ja" to stringResource(R.string.language_japanese),
                                "ko" to stringResource(R.string.language_korean)
                            )

                            val currentSelectedLabel = languageOptions.firstOrNull {
                                it.first == appLanguage || (it.first == "system" && (appLanguage.isEmpty() || appLanguage == "system"))
                            }?.second ?: stringResource(R.string.system_default_language)

                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    onClick = { languageDropdownExpanded = !languageDropdownExpanded },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF1D1B20),
                                    border = BorderStroke(1.dp, Color(0xFF49454F)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("language_dropdown_selector")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = currentSelectedLabel,
                                            color = Color(0xFFE6E1E5),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Icon(
                                            imageVector = if (languageDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFFD0BCFF)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = languageDropdownExpanded,
                                    onDismissRequest = { languageDropdownExpanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(Color(0xFF2B2930))
                                        .border(1.dp, Color(0xFF49454F), RoundedCornerShape(12.dp))
                                ) {
                                    languageOptions.forEach { (code, label) ->
                                        val isSelected = (appLanguage == code) || (code == "system" && (appLanguage.isEmpty() || appLanguage == "system"))
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFFE6E1E5),
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                AudioEffectManager.setAppLanguage(code)
                                                languageDropdownExpanded = false
                                                (context as? android.app.Activity)?.recreate()
                                            },
                                            leadingIcon = if (isSelected) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color(0xFFD0BCFF),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else null,
                                            modifier = Modifier.background(
                                                if (isSelected) Color(0xFF49454F).copy(alpha = 0.5f) else Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }



                    Spacer(modifier = Modifier.height(24.dp))

                    // Developer & Legal Section
                    Text(
                        text = "DEVELOPER & LEGAL",
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Developer Website Link Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val devUrl = BuildConfig.DEVELOPER_WEBSITE_URL
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(devUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                .testTag("developer_website_button"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF49454F))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Developer Website",
                                            color = Color(0xFFE6E1E5),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = BuildConfig.DEVELOPER_WEBSITE_URL,
                                            color = Color(0xFFD0BCFF),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = "Open Developer Website",
                                    tint = Color(0xFFCAC4D0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Privacy Policy & Terms Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPrivacyTermsDialog = true
                                }
                                .testTag("privacy_terms_button"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF49454F))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Policy,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Privacy Policy & Terms",
                                            color = Color(0xFFE6E1E5),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "View privacy terms & data handling disclosures",
                                            color = Color(0xFFCAC4D0),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Privacy Policy & Terms",
                                    tint = Color(0xFFCAC4D0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // GDPR & Ad Privacy Settings Card
                        val isPersonalizedConsent by AudioEffectManager.isPersonalizedAdsConsent.collectAsState()
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gdpr_privacy_settings_card"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF49454F))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFFD0BCFF),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Personalized Ads (GDPR)",
                                                color = Color(0xFFE6E1E5),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (isPersonalizedConsent) "Consent: Granted (Personalized)" else "Consent: Non-personalized Ads Only",
                                                color = Color(0xFFCAC4D0),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isPersonalizedConsent,
                                        onCheckedChange = {
                                            AudioEffectManager.setPersonalizedAdsConsent(it)
                                            AudioEffectManager.setAdConsentStatus(if (it) "GRANTED" else "DENIED")
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFD0BCFF),
                                            checkedTrackColor = Color(0xFF49454F)
                                        ),
                                        modifier = Modifier.testTag("settings_personalized_ads_toggle")
                                    )
                                }
                                if (AdConsentManager.isUmpAvailable()) {
                                    Divider(color = Color(0xFF49454F), thickness = 1.dp)
                                    TextButton(
                                        onClick = {
                                            (context as? android.app.Activity)?.let { act ->
                                                AdConsentManager.requestConsentInfoUpdate(act)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("update_gdpr_consent_button")
                                    ) {
                                        Text(
                                            text = "Update GDPR Consent Preferences",
                                            color = Color(0xFFD0BCFF),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Open Source License (GPLv3) Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLicenseDialog = true
                                }
                                .testTag("open_source_license_button"),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF49454F))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Open Source License",
                                            color = Color(0xFFE6E1E5),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "GNU General Public License v3.0 (GPLv3)",
                                            color = Color(0xFFCAC4D0),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open License Page",
                                    tint = Color(0xFFCAC4D0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Start Onboarding Replay Button
                    OutlinedButton(
                        onClick = {
                            showSettings = false
                            showOnboardingManually = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("replay_onboarding_button"),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Replay Quick Start Guide",
                            color = Color(0xFFD0BCFF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // About section
                    Text(
                        text = "ABOUT",
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2930)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Vibe Amplifier v1.0",
                                color = Color(0xFFE6E1E5),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "High-Fidelity global sound booster and 5-band equalizer for Android.",
                                color = Color(0xFFCAC4D0),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Floating Overlay Permission Request Explanation Dialog
    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = {
                Text(
                    text = "Overlay Permission Required",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "To allow rapid volume adjustments inside other applications, please enable the 'Display over other apps' setting for Volume Booster.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionExplanation = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Grant Permission", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionExplanation = false }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1C28)
        )
    }



    // Privacy Policy & Terms Page Dialog
    if (showPrivacyTermsDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyTermsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Privacy Policy & Terms",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Voluntary Development & Service",
                        color = Color(0xFFD0BCFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Milkys Sound Booster & EQ is an independent, voluntarily developed application provided to enhance device volume and frequency response. The app is provided 'as-is' for personal utility.",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Text(
                        text = "2. Data Privacy & Processing",
                        color = Color(0xFFD0BCFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Zero Personal Data Collection: Milkys App does not collect, record, transmit, or store any personal user identifiers, location data, or private audio recordings.\n" +
                                "• On-Device Processing: All audio amplification and 5-band graphic equalization occur 100% locally on your device via standard Android system DSP engines.\n" +
                                "• Local Storage: User preferences (volume boost levels, equalizer band profiles, theme settings) are saved strictly inside private local device storage.",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Text(
                        text = "3. Google AdMob Advertising",
                        color = Color(0xFFD0BCFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Optional banner advertisements are served via the Google AdMob SDK to help sustain voluntary development and server infrastructure. Google AdMob may process non-personalized diagnostic metrics in accordance with Google's Privacy Policy. Users can disable ads anytime in the app Settings menu.",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Text(
                        text = "4. Device Permissions Disclosure",
                        color = Color(0xFFD0BCFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Modify Audio Settings: Required to modify equalization frequency bands and master output gains.\n" +
                                "• Foreground Service & Tile: Keeps volume processing active when minimized and supports Android notification drawer controls.\n" +
                                "• System Overlay: Optional permission used solely to render floating overlay volume controls above active apps.",
                        color = Color(0xFFE6E1E5),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Privacy Policy Web Link Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val privacyUrl = BuildConfig.PRIVACY_POLICY_URL
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            .testTag("privacy_policy_web_link"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Official Privacy Policy Web Page",
                                    color = Color(0xFFD0BCFF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = BuildConfig.PRIVACY_POLICY_URL,
                                    color = Color(0xFFCAC4D0),
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open Privacy Policy URL",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrivacyTermsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                    modifier = Modifier.testTag("close_privacy_terms_button")
                ) {
                    Text("Close", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF2B2930),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Open Source License Page Dialog (GPLv3 & GitHub Repo Link)
    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Open Source License",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GitHub Repository Link Button with Icon
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val githubUrl = BuildConfig.GITHUB_REPO_URL
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            .testTag("github_repo_link_button"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFD0BCFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "GitHub Repository",
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = "GitHub Repository",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = BuildConfig.GITHUB_REPO_URL,
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open GitHub Repo",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "GNU GENERAL PUBLIC LICENSE v3.0",
                        color = Color(0xFFD0BCFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    // Scrollable GPLv3 License Text Container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        color = Color(0xFF1C1B1F),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF49454F))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = "GNU GENERAL PUBLIC LICENSE\n" +
                                        "Version 3, 29 June 2007\n\n" +
                                        "Copyright (C) 2007 Free Software Foundation, Inc. <https://fsf.org/>\n" +
                                        "Everyone is permitted to copy and distribute verbatim copies of this license document, but changing it is not allowed.\n\n" +
                                        "Preamble\n\n" +
                                        "The GNU General Public License is a free, copyleft license for software and other kinds of works.\n\n" +
                                        "The licenses for most software and other practical works are designed to take away your freedom to share and change the works. By contrast, the GNU General Public License is intended to guarantee your freedom to share and change all versions of a program--to make sure it remains free software for all its users. We, the Free Software Foundation, use the GNU General Public License for most of our software; it applies also to any other work released this way by its authors. You can apply it to your programs, too.\n\n" +
                                        "When we speak of free software, we are referring to freedom, not price. Our General Public Licenses are designed to make sure that you have the freedom to distribute copies of free software, that you receive source code or can get it if you want it, that you can change the software, and that you know you can do these things.\n\n" +
                                        "TERMS AND CONDITIONS\n\n" +
                                        "0. Definitions.\n" +
                                        "\"This License\" refers to version 3 of the GNU General Public License.\n" +
                                        "\"The Program\" refers to any copyrightable work licensed under this License. Each licensee is addressed as \"you\".\n\n" +
                                        "1. Source Code.\n" +
                                        "The \"source code\" for a work means the preferred form of the work for making modifications to it. \"Object code\" means any non-source form of a work.\n\n" +
                                        "2. Basic Permissions.\n" +
                                        "All rights granted under this License are granted for the term of copyright on the Program, and are irrevocable provided the stated conditions are met.\n\n" +
                                        "3. Conveying Verbatim Copies.\n" +
                                        "You may convey verbatim copies of the Program's source code as you receive it, in any medium, provided that you conspicuously and appropriately publish on each copy an appropriate copyright notice.\n\n" +
                                        "4. Conveying Modified Source Versions.\n" +
                                        "You may convey a work based on the Program, or the modifications to produce it from the Program, in the form of source code under the terms of section 3, provided that you also license the entire work under this License.\n\n" +
                                        "5. Disclaimer of Warranty.\n" +
                                        "THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS PROVIDE THE PROGRAM \"AS IS\" WITHOUT WARRANTY OF ANY KIND.",
                                color = Color(0xFFE6E1E5),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLicenseDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                    modifier = Modifier.testTag("close_license_button")
                ) {
                    Text("Close", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF2B2930),
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Quick Start Onboarding Flow (First-time launch or manual triggering)
    if (!hasSeenOnboarding || showOnboardingManually) {
        OnboardingQuickStartDialog(
            onDismiss = {
                AudioEffectManager.setHasSeenOnboarding(true)
                showOnboardingManually = false
            }
        )
    }
}

@Composable
fun AudioVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val barCount = 20
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_bars")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(55.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val animDelay = i * 65
            val heightRatio by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 400 + (i % 4) * 120,
                            delayMillis = animDelay,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_height"
                )
            } else {
                remember { mutableStateOf(0.12f) }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightRatio)
                    .background(
                        color = Color(0xFFD0BCFF),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

fun showHearingWarningDismissedNotification(context: Context) {
    val channelId = "hearing_warning_channel"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Safety Warnings",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for safety warnings and app preferences"
        }
        notificationManager.createNotificationChannel(channel)
    }

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val detailMessage = "Hearing warning is hidden for 7 days. To permanently disable or re-enable it, go to App Settings."

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("Hearing Warning Hidden")
        .setContentText("Warning hidden for 7 days. Go to Settings to manage safety warnings.")
        .setStyle(NotificationCompat.BigTextStyle().bigText(detailMessage))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    try {
        notificationManager.notify(3001, notification)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    Toast.makeText(
        context,
        "Hearing warning hidden for 7 days. Go to Settings to permanently disable or re-enable it.",
        Toast.LENGTH_LONG
    ).show()
}

@Composable
fun AdaptiveBannerAdCard(
    cardColor: Color,
    borderDivider: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryAccent: Color,
    modifier: Modifier = Modifier
) {
    val isAdsIncluded = remember {
        try {
            BuildConfig.INCLUDE_GOOGLE_ADS.toString().toBoolean()
        } catch (e: Exception) {
            true
        }
    }
    val isAdsEnabled by AudioEffectManager.isAdsEnabled.collectAsState()
    val isPersonalizedConsent by AudioEffectManager.isPersonalizedAdsConsent.collectAsState()

    if (!isAdsIncluded || !isAdsEnabled) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("adaptive_banner_ad_card"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderDivider)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            factory = { ctx ->
                val frameLayout = android.widget.FrameLayout(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
                try {
                    val adViewClass = Class.forName("com.google.android.gms.ads.AdView")
                    val adSizeClass = Class.forName("com.google.android.gms.ads.AdSize")
                    val adRequestClass = Class.forName("com.google.android.gms.ads.AdRequest")
                    val adRequestBuilderClass = Class.forName("com.google.android.gms.ads.AdRequest\$Builder")

                    val adView = adViewClass.getConstructor(Context::class.java).newInstance(ctx) as android.view.View

                    adViewClass.getMethod("setAdUnitId", String::class.java).invoke(adView, "ca-app-pub-3940256099942544/6300978111")

                    val displayMetrics = ctx.resources.displayMetrics
                    val adWidthPixels = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
                        windowManager?.currentWindowMetrics?.bounds?.width() ?: displayMetrics.widthPixels
                    } else {
                        displayMetrics.widthPixels
                    }
                    val density = displayMetrics.density
                    val adWidth = (adWidthPixels / density).toInt()

                    val adSizeMethod = adSizeClass.getMethod("getCurrentOrientationAnchoredAdaptiveBannerAdSize", Context::class.java, Int::class.javaPrimitiveType)
                    val adaptiveAdSize = adSizeMethod.invoke(null, ctx, adWidth)

                    adViewClass.getMethod("setAdSize", adSizeClass).invoke(adView, adaptiveAdSize)

                    val adReqBuilder = adRequestBuilderClass.getConstructor().newInstance()
                    if (!isPersonalizedConsent) {
                        try {
                            val extras = android.os.Bundle()
                            extras.putString("npa", "1")
                            val addNetworkExtrasMethod = adRequestBuilderClass.getMethod(
                                "addNetworkExtrasBundle",
                                Class.forName("com.google.ads.mediation.admob.AdMobAdapter"),
                                android.os.Bundle::class.java
                            )
                            addNetworkExtrasMethod.invoke(
                                adReqBuilder,
                                Class.forName("com.google.ads.mediation.admob.AdMobAdapter"),
                                extras
                            )
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                    val adReq = adRequestBuilderClass.getMethod("build").invoke(adReqBuilder)

                    adViewClass.getMethod("loadAd", adRequestClass).invoke(adView, adReq)

                    frameLayout.addView(adView)
                } catch (e: Throwable) {
                    val fallbackLayout = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER
                        setPadding(12, 10, 12, 10)

                        val badge = android.widget.TextView(ctx).apply {
                            text = " SPONSORED AD "
                            textSize = 10f
                            setTextColor(android.graphics.Color.parseColor("#D0BCFF"))
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setBackgroundColor(android.graphics.Color.parseColor("#362F4A"))
                            setPadding(8, 4, 8, 4)
                        }
                        val title = android.widget.TextView(ctx).apply {
                            text = "Adaptive Equalizer Boost & Audio Tuning"
                            textSize = 12f
                            setTextColor(android.graphics.Color.parseColor("#E6E1E5"))
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setPadding(12, 0, 0, 0)
                        }
                        addView(badge)
                        addView(title)
                    }
                    frameLayout.addView(fallbackLayout)
                }
                frameLayout
            }
        )
    }
}

@Composable
fun NativeAdCard(
    cardColor: Color,
    borderDivider: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryAccent: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAdsIncluded = remember {
        try {
            BuildConfig.INCLUDE_GOOGLE_ADS.toString().toBoolean()
        } catch (e: Exception) {
            true
        }
    }
    val isAdsEnabled by AudioEffectManager.isAdsEnabled.collectAsState()
    val isPersonalizedConsent by AudioEffectManager.isPersonalizedAdsConsent.collectAsState()

    if (!isAdsIncluded || !isAdsEnabled) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("native_ad_card"),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderDivider)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            factory = { ctx ->
                val frameLayout = android.widget.FrameLayout(ctx)
                try {
                    val adLoaderClass = Class.forName("com.google.android.gms.ads.AdLoader")
                    val adLoaderBuilderClass = Class.forName("com.google.android.gms.ads.AdLoader\$Builder")
                    val adRequestClass = Class.forName("com.google.android.gms.ads.AdRequest")
                    val adRequestBuilderClass = Class.forName("com.google.android.gms.ads.AdRequest\$Builder")
                    val nativeAdClass = Class.forName("com.google.android.gms.ads.nativead.NativeAd")
                    val nativeAdViewClass = Class.forName("com.google.android.gms.ads.nativead.NativeAdView")

                    val nativeAdView = nativeAdViewClass.getConstructor(Context::class.java).newInstance(ctx) as android.view.ViewGroup

                    val rootLayout = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }

                    val headerRow = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    val adBadge = android.widget.TextView(ctx).apply {
                        text = " Ad "
                        textSize = 10f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#6750A4"))
                        setPadding(10, 4, 10, 4)
                    }

                    val adTitle = android.widget.TextView(ctx).apply {
                        text = "Sponsored Audio Tool"
                        textSize = 14f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        setTextColor(android.graphics.Color.parseColor("#E6E1E5"))
                        setPadding(16, 0, 0, 0)
                    }

                    headerRow.addView(adBadge)
                    headerRow.addView(adTitle)

                    val adBody = android.widget.TextView(ctx).apply {
                        text = "High clarity sound booster & decibel amplifier tools."
                        textSize = 12f
                        setTextColor(android.graphics.Color.parseColor("#CAC4D0"))
                        setPadding(0, 8, 0, 8)
                    }

                    val ctaButton = android.widget.Button(ctx).apply {
                        text = "LEARN MORE"
                        textSize = 12f
                        setTextColor(android.graphics.Color.WHITE)
                        setBackgroundColor(android.graphics.Color.parseColor("#6750A4"))
                    }

                    rootLayout.addView(headerRow)
                    rootLayout.addView(adBody)
                    rootLayout.addView(ctaButton)

                    nativeAdView.addView(rootLayout)

                    nativeAdViewClass.getMethod("setHeadlineView", android.view.View::class.java).invoke(nativeAdView, adTitle)
                    nativeAdViewClass.getMethod("setBodyView", android.view.View::class.java).invoke(nativeAdView, adBody)
                    nativeAdViewClass.getMethod("setCallToActionView", android.view.View::class.java).invoke(nativeAdView, ctaButton)

                    val builderInstance = adLoaderBuilderClass.getConstructor(Context::class.java, String::class.java)
                        .newInstance(ctx, "ca-app-pub-3940256099942544/2247696110")

                    val listenerClass = Class.forName("com.google.android.gms.ads.nativead.NativeAd\$OnNativeAdLoadedListener")
                    val proxyInvocationHandler = java.lang.reflect.InvocationHandler { _, method, args ->
                        if (method.name == "onNativeAdLoaded") {
                            val loadedAd = args[0]
                            try {
                                nativeAdViewClass.getMethod("setNativeAd", nativeAdClass).invoke(nativeAdView, loadedAd)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                        null
                    }
                    val proxyListener = java.lang.reflect.Proxy.newProxyInstance(
                        listenerClass.classLoader,
                        arrayOf(listenerClass),
                        proxyInvocationHandler
                    )

                    adLoaderBuilderClass.getMethod("forNativeAd", listenerClass).invoke(builderInstance, proxyListener)
                    val adLoader = adLoaderBuilderClass.getMethod("build").invoke(builderInstance)

                    val adReqBuilder = adRequestBuilderClass.getConstructor().newInstance()
                    if (!isPersonalizedConsent) {
                        try {
                            val extras = android.os.Bundle()
                            extras.putString("npa", "1")
                            val addNetworkExtrasMethod = adRequestBuilderClass.getMethod(
                                "addNetworkExtrasBundle",
                                Class.forName("com.google.ads.mediation.admob.AdMobAdapter"),
                                android.os.Bundle::class.java
                            )
                            addNetworkExtrasMethod.invoke(
                                adReqBuilder,
                                Class.forName("com.google.ads.mediation.admob.AdMobAdapter"),
                                extras
                            )
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                    val adReq = adRequestBuilderClass.getMethod("build").invoke(adReqBuilder)

                    adLoaderClass.getMethod("loadAd", adRequestClass).invoke(adLoader, adReq)

                    frameLayout.addView(nativeAdView)
                } catch (e: Throwable) {
                    val fallbackView = android.widget.LinearLayout(ctx).apply {
                        orientation = android.widget.LinearLayout.VERTICAL
                        setPadding(12, 12, 12, 12)

                        val badge = android.widget.TextView(ctx).apply {
                            text = " SPONSORED AD "
                            textSize = 10f
                            setTextColor(android.graphics.Color.parseColor("#D0BCFF"))
                            setTypeface(null, android.graphics.Typeface.BOLD)
                        }
                        val title = android.widget.TextView(ctx).apply {
                            text = "Enhance Your Audio Quality"
                            textSize = 14f
                            setTextColor(android.graphics.Color.parseColor("#E6E1E5"))
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setPadding(0, 6, 0, 2)
                        }
                        val desc = android.widget.TextView(ctx).apply {
                            text = "Discover high performance sound tools and audio equalizer settings."
                            textSize = 12f
                            setTextColor(android.graphics.Color.parseColor("#CAC4D0"))
                        }
                        addView(badge)
                        addView(title)
                        addView(desc)
                    }
                    frameLayout.addView(fallbackView)
                }

                frameLayout
            }
        )
    }
}

@Composable
fun OnboardingQuickStartDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isPersonalizedAdsConsent by AudioEffectManager.isPersonalizedAdsConsent.collectAsState()
    var tempPersonalizedConsent by remember { mutableStateOf(isPersonalizedAdsConsent) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth(0.88f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF2B2930),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // App Logo Header
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFD0BCFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Text(
                    text = "Welcome to Milkys App",
                    color = Color(0xFFE6E1E5),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color(0xFF49454F), thickness = 1.dp)

                // Hearing Loss Safety Warning Banner (Ultra-compact)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF382300)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFC107))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Safety Warning",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CAUTION: High volume can damage hearing or speakers. Boost responsibly.",
                            color = Color(0xFFFFD54F),
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Feature Highlights (Concise 3 key features)
                OnboardingFeatureItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Loudness Booster",
                    description = "Amplify audio volume up to +100%"
                )

                OnboardingFeatureItem(
                    icon = Icons.Default.Equalizer,
                    title = "5-Band Equalizer",
                    description = "Fine-tune sound & audio presets"
                )

                OnboardingFeatureItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Quick Controls",
                    description = "Adjust gain via notification or widget"
                )

                // GDPR Privacy & Ad Consent Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_gdpr_consent_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF49454F))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Privacy & Consent",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Privacy & Admob Consent",
                                color = Color(0xFFE6E1E5),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "We use Google AdMob to show ads supporting development. You can choose whether to allow personalized or non-personalized ads.",
                            color = Color(0xFFCAC4D0),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Personalized Ads",
                                color = Color(0xFFE6E1E5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = tempPersonalizedConsent,
                                onCheckedChange = { tempPersonalizedConsent = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFD0BCFF),
                                    checkedTrackColor = Color(0xFF49454F)
                                ),
                                modifier = Modifier.testTag("onboarding_personalized_ads_toggle")
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Read Privacy Policy",
                                color = Color(0xFFD0BCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Privacy Policy",
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = {
                        AudioEffectManager.setPersonalizedAdsConsent(tempPersonalizedConsent)
                        AudioEffectManager.setAdConsentStatus(if (tempPersonalizedConsent) "GRANTED" else "DENIED")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("onboarding_get_started_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "GET STARTED",
                        color = Color(0xFF381E72),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF49454F), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFD0BCFF),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFE6E1E5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = Color(0xFFCAC4D0),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun AppHeaderRow(
    windowSizeGroup: WindowSizeGroup,
    textPrimary: Color,
    textSecondary: Color,
    primaryAccent: Color,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOnboarding: () -> Unit,
    onSoundTest: () -> Unit,
    isSoundTesting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Milkys App Logo",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, primaryAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
            Column {
                Text(
                    text = "MILKYS APP",
                    color = textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Audio Booster & EQ",
                    color = textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (windowSizeGroup == WindowSizeGroup.EXPANDED) {
                IconButton(
                    onClick = onOpenOnboarding,
                    modifier = Modifier.testTag("expanded_help_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Quick Start Guide",
                        tint = primaryAccent
                    )
                }
            }

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier.testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme Mode",
                    tint = primaryAccent
                )
            }

            Surface(
                onClick = onOpenSettings,
                shape = CircleShape,
                color = primaryAccent.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(50.dp)
                    .testTag("settings_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = primaryAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}



@Composable
fun DecibelBoosterCard(
    isEnabled: Boolean,
    boostProgress: Int,
    isSoundTesting: Boolean,
    isSliderStepped: Boolean,
    cardColor: Color,
    borderDivider: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryAccent: Color,
    dialBgColor: Color,
    onSoundTest: () -> Unit,
    onTogglePower: () -> Unit,
    onBoostChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderDivider)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(R.string.decibel_booster_title),
                color = textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = onSoundTest,
                        enabled = !isSoundTesting,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("sound_test_button"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (isSoundTesting) primaryAccent else borderDivider,
                            contentColor = if (isSoundTesting) Color.White else primaryAccent
                        )
                    ) {
                        Icon(
                            imageVector = if (isSoundTesting) Icons.Default.VolumeUp else Icons.Default.GraphicEq,
                            contentDescription = "Sound Test",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isSoundTesting) "3s..." else stringResource(R.string.sound_test_3sec),
                        color = if (isSoundTesting) primaryAccent else textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier.size(118.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ring_glow")
                    val ringScale by if (isEnabled && boostProgress > 0) {
                        infiniteTransition.animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = EaseInOut),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                    } else {
                        remember { mutableStateOf(1.0f) }
                    }

                    Box(
                        modifier = Modifier
                            .scale(ringScale)
                            .size(106.dp)
                            .border(
                                width = 6.dp,
                                color = if (isEnabled) primaryAccent else borderDivider,
                                shape = CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .shadow(6.dp, CircleShape)
                            .background(dialBgColor, CircleShape)
                            .clip(CircleShape)
                            .clickable { onTogglePower() }
                            .testTag("power_dial"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Toggle Booster Power",
                                tint = if (isEnabled) primaryAccent else textSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEnabled) "+${boostProgress}%" else "POWER",
                                color = if (isEnabled) textPrimary else textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = stringResource(R.string.amplification_level_title),
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isSliderStepped) "10% Step Mode" else "Free Slider Mode",
                            color = primaryAccent,
                            fontSize = 10.sp
                        )
                    }
                    Surface(
                        color = primaryAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "+$boostProgress%",
                            color = if (isEnabled) primaryAccent else textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Slider(
                    value = boostProgress.toFloat(),
                    onValueChange = { rawProgress ->
                        val valInt = if (isSliderStepped) {
                            (kotlin.math.round(rawProgress / 10f) * 10).toInt()
                        } else {
                            rawProgress.toInt()
                        }
                        onBoostChange(valInt)
                    },
                    steps = if (isSliderStepped) 9 else 0,
                    valueRange = 0f..100f,
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = primaryAccent,
                        activeTrackColor = primaryAccent,
                        inactiveTrackColor = borderDivider
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("boost_slider")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickBoostPresetsCard(
    isEnabled: Boolean,
    boostProgress: Int,
    cardColor: Color,
    borderDivider: Color,
    textSecondary: Color,
    primaryAccent: Color,
    onSelectPreset: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, borderDivider)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.quick_presets_title),
                color = textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    "MUTE" to 0,
                    "30%" to 30,
                    "60%" to 60,
                    "100%" to 100,
                    "MAX" to 100
                )
                presets.forEach { (label, value) ->
                    val isSelected = isEnabled && boostProgress == value && (label != "MUTE" || boostProgress == 0)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPreset(value) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryAccent,
                            selectedLabelColor = Color.White,
                            containerColor = borderDivider,
                            labelColor = textSecondary
                        ),
                        modifier = Modifier.testTag("boost_preset_$label")
                    )
                }
            }
        }
    }
}

@Composable
fun HearingWarningCard(
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF382300)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, Color(0xFFFFC107))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Hearing Loss Safety Warning",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.hearing_warning_title),
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stringResource(R.string.hearing_warning_desc),
                    color = Color(0xFFFFF59D),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("close_hearing_warning_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hide warning for 7 days",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}



@Composable
fun VisualEqualizerCard(
    isEnabled: Boolean,
    eqBands: IntArray,
    currentPreset: String,
    defaultPreset: String,
    customPresets: Map<String, IntArray>,
    cardColor: Color,
    borderDivider: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryAccent: Color,
    onBandChange: (Int, Int) -> Unit,
    onApplyPreset: (String) -> Unit,
    onSaveCustomPreset: (String, IntArray) -> Boolean,
    onDeleteCustomPreset: (String) -> Unit,
    onSetDefaultPreset: (String) -> Unit,
    onExportPreset: (String) -> String,
    onExportAllPresets: () -> String,
    onImportPreset: (String) -> String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, borderDivider)
    ) {
        EqualizerComponent(
            isEnabled = isEnabled,
            eqBands = eqBands,
            currentPreset = currentPreset,
            defaultPreset = defaultPreset,
            customPresets = customPresets,
            cardColor = cardColor,
            borderDivider = borderDivider,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            primaryAccent = primaryAccent,
            onBandChange = onBandChange,
            onApplyPreset = onApplyPreset,
            onSaveCustomPreset = onSaveCustomPreset,
            onDeleteCustomPreset = onDeleteCustomPreset,
            onSetDefaultPreset = onSetDefaultPreset,
            onExportPreset = onExportPreset,
            onExportAllPresets = onExportAllPresets,
            onImportPreset = onImportPreset
        )
    }
}

@Composable
fun EqualizerComponent(
    isEnabled: Boolean,
    eqBands: IntArray,
    currentPreset: String,
    defaultPreset: String,
    customPresets: Map<String, IntArray>,
    cardColor: Color,
    borderDivider: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryAccent: Color,
    onBandChange: (Int, Int) -> Unit,
    onApplyPreset: (String) -> Unit,
    onSaveCustomPreset: (String, IntArray) -> Boolean,
    onDeleteCustomPreset: (String) -> Unit,
    onSetDefaultPreset: (String) -> Unit,
    onExportPreset: (String) -> String,
    onExportAllPresets: () -> String,
    onImportPreset: (String) -> String?
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showSaveDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    var newPresetName by remember { mutableStateOf("") }
    var exportJsonText by remember { mutableStateOf("") }
    var exportTitleText by remember { mutableStateOf("") }
    var importJsonInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row: Title & Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.visual_equalizer_title),
                    color = textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (currentPreset == defaultPreset) "Active: $currentPreset (Default ★)" else "Active: $currentPreset",
                    color = primaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick Action Buttons (Save, Import, Export, Reset)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reload / Reset Bands Button
                Surface(
                    onClick = {
                        if (isEnabled) {
                            for (i in 0 until 5) onBandChange(i, 0)
                        }
                    },
                    enabled = isEnabled,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEnabled) Color(0xFF2B2930) else Color(0xFF1F1D24),
                    border = BorderStroke(1.dp, if (isEnabled) primaryAccent.copy(alpha = 0.5f) else borderDivider.copy(alpha = 0.3f)),
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.content_desc_reset_bands),
                            tint = if (isEnabled) textSecondary else textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Add Custom Preset (+) Button
                Surface(
                    onClick = {
                        if (isEnabled) {
                            val defaultName = "Custom ${customPresets.size + 1}"
                            newPresetName = defaultName.take(10)
                            showSaveDialog = true
                        }
                    },
                    enabled = isEnabled,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEnabled) Color(0xFF2B2930) else Color(0xFF1F1D24),
                    border = BorderStroke(1.dp, if (isEnabled) primaryAccent.copy(alpha = 0.5f) else borderDivider.copy(alpha = 0.3f)),
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.content_desc_save_preset),
                            tint = if (isEnabled) primaryAccent else primaryAccent.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Import Preset Button
                Surface(
                    onClick = {
                        if (isEnabled) {
                            importJsonInput = ""
                            showImportDialog = true
                        }
                    },
                    enabled = isEnabled,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEnabled) Color(0xFF2B2930) else Color(0xFF1F1D24),
                    border = BorderStroke(1.dp, if (isEnabled) primaryAccent.copy(alpha = 0.5f) else borderDivider.copy(alpha = 0.3f)),
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = stringResource(R.string.content_desc_import_preset),
                            tint = if (isEnabled) primaryAccent else primaryAccent.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Export All Presets Button
                Surface(
                    onClick = {
                        if (isEnabled) {
                            exportJsonText = onExportAllPresets()
                            exportTitleText = "All Presets Backup"
                            showExportDialog = true
                        }
                    },
                    enabled = isEnabled,
                    shape = RoundedCornerShape(12.dp),
                    color = if (isEnabled) Color(0xFF2B2930) else Color(0xFF1F1D24),
                    border = BorderStroke(1.dp, if (isEnabled) primaryAccent.copy(alpha = 0.5f) else borderDivider.copy(alpha = 0.3f)),
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = stringResource(R.string.content_desc_export_presets),
                            tint = if (isEnabled) primaryAccent else primaryAccent.copy(alpha = 0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 5-Band Dynamic Frequency Sliders
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val frequencies = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")
            for (i in 0 until 5) {
                val level = eqBands.getOrElse(i) { 0 }

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Gain Readout Display
                    Text(
                        text = "${if (level > 0) "+" else ""}$level dB",
                        color = when {
                            !isEnabled -> Color(0xFFCAC4D0).copy(alpha = 0.5f)
                            level > 0 -> Color(0xFF81C784)
                            level < 0 -> Color(0xFFFFB74D)
                            else -> primaryAccent
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Vertical Slider / Track Component
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                            .width(48.dp)
                            .background(Color(0xFF2B2930), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Increment (+) Button
                            IconButton(
                                onClick = {
                                    if (isEnabled && level < 15) onBandChange(i, level + 1)
                                },
                                enabled = isEnabled,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .padding(2.dp)
                                    .background(
                                        if (isEnabled) Color(0xFF4F378B) else Color(0xFF36343B),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.content_desc_increase_band, i + 1),
                                    tint = if (isEnabled) Color.White else Color(0xFFCAC4D0).copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Dynamic Fill Bar Gauge with Vertical Drag support
                            var accumulatedDrag by remember { mutableFloatStateOf(0f) }
                            Box(
                                modifier = Modifier
                                    .height(80.dp)
                                    .width(16.dp)
                                    .background(Color(0xFF1D1B20), RoundedCornerShape(8.dp))
                                    .pointerInput(isEnabled) {
                                        if (isEnabled) {
                                            detectVerticalDragGestures(
                                                onDragStart = { accumulatedDrag = 0f },
                                                onVerticalDrag = { change, dragAmount ->
                                                    change.consume()
                                                    accumulatedDrag -= dragAmount // Dragging UP (negative dragAmount) increases dB
                                                    val stepPixels = 10f
                                                    if (kotlin.math.abs(accumulatedDrag) >= stepPixels) {
                                                        val steps = (accumulatedDrag / stepPixels).toInt()
                                                        val newLevel = (level + steps).coerceIn(-15, 15)
                                                        if (newLevel != level) {
                                                            onBandChange(i, newLevel)
                                                            accumulatedDrag -= steps * stepPixels
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                val fillFraction = ((level + 15) / 30f).coerceIn(0.05f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(fillFraction)
                                        .background(
                                            if (isEnabled) {
                                                Brush.verticalGradient(
                                                    colors = listOf(primaryAccent, Color(0xFF805BFF))
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    colors = listOf(Color(0xFF49454F), Color(0xFF49454F))
                                                )
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                )
                            }

                            // Decrement (-) Button
                            IconButton(
                                onClick = {
                                    if (isEnabled && level > -15) onBandChange(i, level - 1)
                                },
                                enabled = isEnabled,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .padding(2.dp)
                                    .background(
                                        if (isEnabled) Color(0xFF4F378B) else Color(0xFF36343B),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.content_desc_decrease_band, i + 1),
                                    tint = if (isEnabled) Color.White else Color(0xFFCAC4D0).copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Frequency Label
                    Text(
                        text = frequencies[i],
                        color = Color(0xFFCAC4D0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(color = Color(0xFF49454F), thickness = 1.dp)

        // Preset Manager Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sound Profiles",
                modifier = Modifier.weight(1f),
                color = Color(0xFFCAC4D0),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (currentPreset != "Custom") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isDefault = currentPreset == defaultPreset
                    IconButton(
                        onClick = {
                            onSetDefaultPreset(currentPreset)
                            Toast.makeText(context, "Set '$currentPreset' as default preset", Toast.LENGTH_SHORT).show()
                        },
                        enabled = isEnabled,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(R.string.content_desc_set_default_preset),
                            tint = if (isDefault) Color(0xFFFFD54F) else textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            exportJsonText = onExportPreset(currentPreset)
                            exportTitleText = "Preset: $currentPreset"
                            showExportDialog = true
                        },
                        enabled = isEnabled,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = stringResource(R.string.content_desc_export_preset),
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (customPresets.containsKey(currentPreset)) {
                        IconButton(
                            onClick = {
                                onDeleteCustomPreset(currentPreset)
                                Toast.makeText(context, "Deleted preset '$currentPreset'", Toast.LENGTH_SHORT).show()
                            },
                            enabled = isEnabled,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.content_desc_delete_preset),
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // LazyRow of Built-In and Custom Preset Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val builtIn = listOf("Flat", "Bass Booster", "Vocal Booster", "Rock", "Pop", "Jazz")
            val allPresets = builtIn + customPresets.keys.filter { !builtIn.contains(it) }

            items(allPresets) { preset ->
                val isSelected = currentPreset == preset
                val isDefault = defaultPreset == preset
                val isCustom = customPresets.containsKey(preset)

                Box(
                    modifier = Modifier
                        .background(
                            color = when {
                                isSelected -> primaryAccent
                                isCustom -> Color(0xFF332D41)
                                else -> Color(0xFF49454F)
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            width = if (isDefault) 1.5.dp else 0.dp,
                            color = if (isDefault) Color(0xFFFFD54F) else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(enabled = isEnabled) {
                            onApplyPreset(preset)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("preset_$preset"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isDefault) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Default",
                                tint = if (isSelected) Color(0xFF381E72) else Color(0xFFFFD54F),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = preset,
                            color = if (isSelected) Color(0xFF381E72) else if (isEnabled) Color(0xFFE6E1E5) else Color(0xFFCAC4D0).copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Save Custom Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enter a unique profile name for your current 5-band gain settings:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { if (it.length <= 10) newPresetName = it },
                        label = { Text("Preset Name (Max 10 chars)") },
                        supportingText = {
                            Text(
                                text = "${newPresetName.length}/10",
                                color = if (newPresetName.length >= 10) Color(0xFFFFB74D) else textSecondary,
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Bands: ${eqBands.joinToString { if (it > 0) "+$it" else "$it" }} dB",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            onSaveCustomPreset(newPresetName, eqBands)
                            Toast.makeText(context, "Saved custom preset '$newPresetName'", Toast.LENGTH_SHORT).show()
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("Save Preset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Preset Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export JSON - $exportTitleText") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Copy or share this preset payload to transfer sound profiles across devices:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = exportJsonText,
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportJsonText))
                        Toast.makeText(context, "Preset copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Import Preset Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import Preset JSON") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Paste a preset JSON or backup payload below:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        placeholder = { Text("{\"name\": \"My Preset\", \"bands\": [8, 5, 2, 0, 0]}") },
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                val text = clipboardManager.getText()?.text
                                if (!text.isNullOrEmpty()) {
                                    importJsonInput = text
                                } else {
                                    Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Clipboard", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val importedName = onImportPreset(importJsonInput)
                        if (importedName != null) {
                            Toast.makeText(context, "Successfully imported '$importedName'!", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(context, "Invalid JSON preset format. Please check payload.", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Import Preset", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SystemBatteryDiagnosticCard(
    isBatterySaverOn: Boolean,
    isBatteryOptimized: Boolean,
    context: Context
) {
    if (isBatterySaverOn || isBatteryOptimized) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF31111D)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF93000A))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Battery optimization warning",
                        tint = Color(0xFFFFB4AB),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.system_battery_diagnostic_title),
                        color = Color(0xFFFFB4AB),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isBatterySaverOn) {
                        "Battery Saver is currently ACTIVE. The Android OS will severely restrict background CPU processes and close low-latency audio enhancements."
                    } else {
                        "The application is subject to Android Battery Optimizations. Please grant unrestricted background use to prevent abrupt service closures."
                    },
                    color = Color(0xFFFFB4AB).copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF93000A)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("battery_optimize_button")
                ) {
                    Text(
                        text = "Fix Settings",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2F22)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF386A42))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Background service safe status",
                    tint = Color(0xFFB4E6B9),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Background Optimization Safe",
                        color = Color(0xFFB4E6B9),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "System has granted maximum background stability with zero latency.",
                        color = Color(0xFFB4E6B9).copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

