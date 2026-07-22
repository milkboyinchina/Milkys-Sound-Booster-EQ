package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        AudioEffectManager.init(this)
        
        // Start background service on launch if enabled
        if (AudioEffectManager.isBoostEnabled.value) {
            startBoosterService()
        }

        // Initialize Google Mobile Ads SDK if included in the build
        val isAdsIncluded = try {
            BuildConfig.INCLUDE_GOOGLE_ADS.toString().toBoolean()
        } catch (e: Exception) {
            true
        }
        if (isAdsIncluded) {
            try {
                com.google.android.gms.ads.MobileAds.initialize(this) {}
            } catch (e: Exception) {
                e.printStackTrace()
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

    var showSettings by remember { mutableStateOf(false) }
    var showOnboardingManually by remember { mutableStateOf(false) }
    var isSoundTesting by remember { mutableStateOf(false) }
    var showAdsDisableNoticeDialog by remember { mutableStateOf(false) }
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
            } finally {
                withContext(Dispatchers.Main) {
                    isSoundTesting = false
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "MILKYS APP",
                                color = textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "High-Fidelity Audio Boost & Equalizer",
                                color = textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { AudioEffectManager.setDarkTheme(!isDarkTheme) },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme Mode",
                                    tint = primaryAccent
                                )
                            }

                            IconButton(
                                onClick = { showSettings = true },
                                modifier = Modifier.testTag("settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Open Settings",
                                    tint = primaryAccent
                                )
                            }
                        }
                    }
                }

        // Bouncing Audio Waveform Visualizer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderDivider)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ACOUSTIC FEEDBACK",
                        color = textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    AudioVisualizer(isPlaying = isEnabled)
                }
            }
        }

        // Circular Dial & Boost Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderDivider)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "DECIBEL BOOSTER",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    // Master Boost Dial Row with Sound Test Button on Left
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Sound Test Button on the left of ON/OFF button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            FilledTonalIconButton(
                                onClick = { playSoundTest3Sec() },
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
                                    contentDescription = "Sound Test 3s",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSoundTesting) "3s Playing..." else "Sound Test",
                                color = if (isSoundTesting) primaryAccent else textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Glowing Master Power Dial
                        Box(
                            modifier = Modifier.size(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Pulsating background ring
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
                                    .size(135.dp)
                                    .border(
                                        width = 8.dp,
                                        color = if (isEnabled) primaryAccent else borderDivider,
                                        shape = CircleShape
                                    )
                            )

                            // Inner click dial to trigger amplifier
                            Box(
                                modifier = Modifier
                                    .size(105.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(dialBgColor, CircleShape)
                                    .clip(CircleShape)
                                    .clickable {
                                        val nextState = !isEnabled
                                        AudioEffectManager.setBoostEnabled(nextState)
                                        if (nextState) onStartService() else onStopService()
                                    }
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
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isEnabled) "+${boostProgress}%" else "POWER",
                                        color = if (isEnabled) textPrimary else textSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Boost percentage slider
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Loudness Amplification",
                                    color = textSecondary,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isSliderStepped) "10% Step Mode" else "Free Slider Mode",
                                    color = primaryAccent,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = "+$boostProgress%",
                                color = if (isEnabled) primaryAccent else textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = boostProgress.toFloat(),
                            onValueChange = { rawProgress ->
                                val valInt = if (isSliderStepped) {
                                    (kotlin.math.round(rawProgress / 10f) * 10).toInt()
                                } else {
                                    rawProgress.toInt()
                                }
                                AudioEffectManager.setBoostProgress(valInt)
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

        // Hearing Loss Safety Warning Card (with 7-day hide close button)
        if (!isHearingWarningDisabled && System.currentTimeMillis() > hearingWarningHiddenUntil) {
            item {
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
                                text = "WARNING: HEARING & SPEAKER DAMAGE RISK",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Over-boosting audio can cause permanent hearing impairment and damage or blow out device speakers and headphones. Please boost responsibly.",
                                color = Color(0xFFFFF59D),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { AudioEffectManager.hideHearingWarningFor7Days() },
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
        }

        // Floating Overlays Card (Placed above Visual Equalizer)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderDivider)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "FLOATING OVERLAY CONTROLS",
                            color = textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Add dynamic floating panel over other apps for rapid gain access.",
                            color = textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isFloatingEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                // Request overlay draw permissions on M+
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                    showPermissionExplanation = true
                                } else {
                                    AudioEffectManager.setFloatingEnabled(true)
                                    onStartService()
                                }
                            } else {
                                AudioEffectManager.setFloatingEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryAccent,
                            checkedTrackColor = borderDivider,
                            uncheckedThumbColor = textSecondary,
                            uncheckedTrackColor = borderDivider
                        ),
                        modifier = Modifier.testTag("floating_toggle")
                    )
                }
            }
        }

        // Equalizer Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, borderDivider)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "VISUAL EQUALIZER",
                        color = textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )

                    // 5-band vertical sliders layout represented beautifully in a horizontal grid with enlarged touch controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
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
                                Text(
                                    text = "${if (level > 0) "+" else ""}$level dB",
                                    color = if (isEnabled) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Custom vertical bar with enlarged +/- adjustment buttons
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(vertical = 4.dp)
                                        .width(42.dp)
                                        .background(Color(0xFF49454F), RoundedCornerShape(21.dp)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Plus adjustments - Enlarged Button
                                        IconButton(
                                            onClick = {
                                                if (isEnabled && level < 15) {
                                                    AudioEffectManager.setBandLevel(i, level + 1)
                                                }
                                            },
                                            enabled = isEnabled,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .padding(2.dp)
                                                .background(
                                                    if (isEnabled) Color(0xFF6750A4) else Color(0xFF36343B),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increase Band",
                                                tint = if (isEnabled) Color.White else Color(0xFFCAC4D0).copy(alpha = 0.4f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        // Vertical track indicator line
                                        Box(
                                            modifier = Modifier
                                                .height(54.dp)
                                                .width(5.dp)
                                                .background(
                                                    if (isEnabled) {
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color(0xFFD0BCFF), Color(0xFFD0BCFF))
                                                        )
                                                    } else {
                                                        Brush.verticalGradient(
                                                            colors = listOf(Color(0xFFCAC4D0), Color(0xFFCAC4D0))
                                                        )
                                                    }
                                                )
                                        )

                                        // Minus adjustments - Enlarged Button
                                        IconButton(
                                            onClick = {
                                                if (isEnabled && level > -15) {
                                                    AudioEffectManager.setBandLevel(i, level - 1)
                                                }
                                            },
                                            enabled = isEnabled,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .padding(2.dp)
                                                .background(
                                                    if (isEnabled) Color(0xFF6750A4) else Color(0xFF36343B),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrease Band",
                                                tint = if (isEnabled) Color.White else Color(0xFFCAC4D0).copy(alpha = 0.4f),
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = frequencies[i],
                                    color = Color(0xFFCAC4D0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFF49454F), thickness = 1.dp)

                    // Presets horizontal selection flow
                    Text(
                        text = "Sound Profiles",
                        color = Color(0xFFCAC4D0),
                        fontSize = 12.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf("Flat", "Bass Booster", "Vocal Booster", "Rock", "Pop", "Jazz")
                        items(presets) { preset ->
                            val isSelected = currentPreset == preset
                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = if (isSelected) {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFFD0BCFF), Color(0xFFD0BCFF))
                                            )
                                        } else {
                                            Brush.linearGradient(
                                                colors = listOf(Color(0xFF49454F), Color(0xFF49454F))
                                            )
                                        },
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable(enabled = isEnabled) {
                                        AudioEffectManager.applyPreset(preset)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("preset_$preset"),
                                contentAlignment = Alignment.Center
                            ) {
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
        }

        // System Optimization Diagnostics (Battery Optimization / Saver check)
        item {
            if (isBatterySaverOn || isBatteryOptimized) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF31111D)), // Dark red/burgundy Clean Minimalism theme
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
                                text = "System Battery Warning",
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
                // Success / background state is perfectly clear
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
            } // End item
            } // End LazyColumn

            // Vertical Scroll Bar Indicator Line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(16.dp)
                    .padding(vertical = 24.dp, horizontal = 4.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                // Scrollbar Track Line
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(borderDivider, RoundedCornerShape(2.dp))
                )

                // Active Scroll Indicator Line
                val scrollPercentage = remember {
                    derivedStateOf {
                        val totalItems = listState.layoutInfo.totalItemsCount
                        if (totalItems <= 1) 0f
                        else {
                            val firstVisible = listState.firstVisibleItemIndex.toFloat()
                            (firstVisible / (totalItems - 1)).coerceIn(0f, 1f)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(48.dp)
                        .align(
                            BiasAlignment(
                                horizontalBias = 0f,
                                verticalBias = (scrollPercentage.value * 2f) - 1f
                            )
                        )
                        .background(primaryAccent, RoundedCornerShape(3.dp))
                )
            }
        } // End Row

        // Place Banner Ad at the very bottom of the screen
        if (isAdsIncluded && isAdsEnabled) {
            BannerAdView(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
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
                        text = "AMPLIFIER & NOTIFICATION CONTROLS",
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
                                        text = "10% Stepped Loudness Slider",
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isSliderStepped) "Slider snaps to 10% increments (0%, 10%, 20%...)." else "Smooth free slider movement.",
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
                                        text = "Notification Bar Controls",
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Display loudness adjustment (-10%, +10%) and OFF button in the notification bar when booster is enabled.",
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
                                        text = "Disable Safety Warning Banner",
                                        color = Color(0xFFE6E1E5),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Completely hide the hearing and speaker damage warning banner on main dashboard.",
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

                    // Ads Setting Section (Rendered only when AdMob is included in build, removed for F-Droid target)
                    if (isAdsIncluded) {
                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "GOOGLE ADMOB PREFERENCE",
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
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Enable Google AdMob",
                                            color = Color(0xFFE6E1E5),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Support developer by showing AdMob banner ads at the bottom.",
                                            color = Color(0xFFCAC4D0),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Switch(
                                        checked = isAdsEnabled && isAdsIncluded,
                                        onCheckedChange = { checked ->
                                            if (!checked) {
                                                showAdsDisableNoticeDialog = true
                                            } else {
                                                AudioEffectManager.setAdsEnabled(true)
                                            }
                                        },
                                        enabled = isAdsIncluded,
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFFD0BCFF),
                                            checkedTrackColor = Color(0xFF49454F),
                                            uncheckedThumbColor = Color(0xFFCAC4D0),
                                            uncheckedTrackColor = Color(0xFF49454F)
                                        ),
                                        modifier = Modifier.testTag("ads_enable_toggle")
                                    )
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

    // Voluntary Development & Developer Support Notice Dialog (When turning off ads)
    if (showAdsDisableNoticeDialog) {
        AlertDialog(
            onDismissRequest = { showAdsDisableNoticeDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Developer Support Notice",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "This application is developed voluntarily as a free utility for the Android audio community.\n\n" +
                            "Ads are completely optional, but keeping them enabled directly supports ongoing voluntary development, updates, and maintenance.\n\n" +
                            "Would you like to keep ads enabled to show support to the developer, or proceed to turn them off?",
                    color = Color(0xFFE6E1E5),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        AudioEffectManager.setAdsEnabled(true)
                        showAdsDisableNoticeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                    modifier = Modifier.testTag("keep_ads_on_button")
                ) {
                    Text("Keep Ads On (Support)", color = Color(0xFF381E72), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        AudioEffectManager.setAdsEnabled(false)
                        showAdsDisableNoticeDialog = false
                    },
                    border = BorderStroke(1.dp, Color(0xFFCAC4D0)),
                    modifier = Modifier.testTag("confirm_disable_ads_button")
                ) {
                    Text("Turn Off Ads", color = Color(0xFFCAC4D0))
                }
            },
            containerColor = Color(0xFF2B2930),
            shape = RoundedCornerShape(24.dp)
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

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("banner_ad_view"),
        factory = { ctx ->
            try {
                com.google.android.gms.ads.AdView(ctx).apply {
                    setAdSize(com.google.android.gms.ads.AdSize.BANNER)
                    // Standard Google AdMob test banner ad unit ID
                    adUnitId = "ca-app-pub-3940256099942544/6300978111"
                    try {
                        loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                android.view.View(ctx)
            }
        }
    )
}

@Composable
fun OnboardingQuickStartDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF2B2930),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon Header
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFD0BCFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Onboarding Welcome Icon",
                        tint = Color(0xFF381E72),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Welcome to Milkys App!",
                    color = Color(0xFFE6E1E5),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Quick Start Guide for High-Fidelity Audio",
                    color = Color(0xFFD0BCFF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Divider(color = Color(0xFF49454F), thickness = 1.dp)

                // Hearing Loss Safety Warning Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF382300)),
                    shape = RoundedCornerShape(16.dp),
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
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "WARNING: HEARING & SPEAKER DAMAGE RISK",
                                color = Color(0xFFFFD54F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Over-boosting audio can permanently damage your hearing and cause physical damage or blown speakers/earphones. Always boost responsibly.",
                                color = Color(0xFFFFF59D),
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Feature Highlights
                OnboardingFeatureItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Decibel Loudness Booster",
                    description = "Tap the central power dial to activate amplification. Adjust gain up to +100% boost."
                )

                OnboardingFeatureItem(
                    icon = Icons.Default.Tune,
                    title = "10% Stepped or Free Slider",
                    description = "In Settings, switch between 10% snapping steps (0%, 10%, 20%...) or continuous free slider mode."
                )

                OnboardingFeatureItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Notification Bar Quick Controls",
                    description = "Control gain (-10%, +10%) and instant OFF directly from your Android notification drawer."
                )

                OnboardingFeatureItem(
                    icon = Icons.Default.Equalizer,
                    title = "5-Band Visual Equalizer",
                    description = "Fine-tune frequencies from 60Hz to 14kHz or select presets like Bass Booster, Rock, Pop, or Jazz."
                )

                OnboardingFeatureItem(
                    icon = Icons.Default.Layers,
                    title = "Floating Overlay Widget",
                    description = "Enable the floating widget to easily adjust volume on top of video or music apps."
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("onboarding_get_started_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD0BCFF)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "GET STARTED",
                        color = Color(0xFF381E72),
                        fontSize = 15.sp,
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
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF49454F), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFD0BCFF),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFE6E1E5),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color(0xFFCAC4D0),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

