package com.milkys.soundbooster

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VolumeBoosterService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "volume_booster_channel"
        
        const val ACTION_START = "com.milkys.soundbooster.action.START"
        const val ACTION_STOP = "com.milkys.soundbooster.action.STOP"
        const val ACTION_TOGGLE_BOOST = "com.milkys.soundbooster.action.TOGGLE_BOOST"
        const val ACTION_BOOST_UP = "com.milkys.soundbooster.action.BOOST_UP"
        const val ACTION_BOOST_DOWN = "com.milkys.soundbooster.action.BOOST_DOWN"
        const val ACTION_BOOST_OFF = "com.milkys.soundbooster.action.BOOST_OFF"
    }

    // Lifecycle variables to host Jetpack Compose in a Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        AudioEffectManager.init(this)
        createNotificationChannel()
        
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Listen for changes in state to update notification
        serviceScope.launch {
            launch {
                AudioEffectManager.isBoostEnabled.collectLatest { enabled ->
                    updateNotification()
                }
            }
            launch {
                AudioEffectManager.boostProgress.collectLatest { progress ->
                    updateNotification()
                }
            }
            launch {
                AudioEffectManager.isNotifControlsEnabled.collectLatest { enabled ->
                    updateNotification()
                }
            }
            launch {
                AudioEffectManager.isFloatingEnabled.collectLatest { enabled ->
                    if (enabled) {
                        showFloatingOverlay()
                    } else {
                        hideFloatingOverlay()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        // Call startForegroundService immediately so Android 8.0+ never throws ForegroundServiceDidNotStartInTimeException
        startForegroundService()

        if (AudioEffectManager.isFloatingEnabled.value) {
            showFloatingOverlay()
        }

        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_TOGGLE_BOOST -> {
                AudioEffectManager.setBoostEnabled(!AudioEffectManager.isBoostEnabled.value)
            }
            ACTION_BOOST_UP -> {
                val step = if (AudioEffectManager.isSliderStepped.value) 10 else 5
                val newBoost = (AudioEffectManager.boostProgress.value + step).coerceAtMost(100)
                AudioEffectManager.setBoostProgress(newBoost)
            }
            ACTION_BOOST_DOWN -> {
                val step = if (AudioEffectManager.isSliderStepped.value) 10 else 5
                val newBoost = (AudioEffectManager.boostProgress.value - step).coerceAtLeast(0)
                AudioEffectManager.setBoostProgress(newBoost)
            }
            ACTION_BOOST_OFF -> {
                AudioEffectManager.setBoostEnabled(false)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        serviceScope.cancel()
        hideFloatingOverlay()
        // AGENTS 5.2 - release AudioEffect handles to prevent audio server crashes
        AudioEffectManager.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Volume Booster Service"
            val descriptionText = "Monitors background audio booster and floating controls"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun updateNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(): Notification {
        val titleIntent = Intent(this, MainActivity::class.java)
        val pendingTitleIntent = PendingIntent.getActivity(
            this, 0, titleIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = Intent(this, VolumeBoosterService::class.java).apply {
            action = ACTION_TOGGLE_BOOST
        }
        val pendingToggleIntent = PendingIntent.getService(
            this, 1, toggleIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val boostUpIntent = Intent(this, VolumeBoosterService::class.java).apply {
            action = ACTION_BOOST_UP
        }
        val pendingBoostUpIntent = PendingIntent.getService(
            this, 3, boostUpIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val boostDownIntent = Intent(this, VolumeBoosterService::class.java).apply {
            action = ACTION_BOOST_DOWN
        }
        val pendingBoostDownIntent = PendingIntent.getService(
            this, 4, boostDownIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val boostOffIntent = Intent(this, VolumeBoosterService::class.java).apply {
            action = ACTION_BOOST_OFF
        }
        val pendingBoostOffIntent = PendingIntent.getService(
            this, 5, boostOffIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, VolumeBoosterService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isEnabled = AudioEffectManager.isBoostEnabled.value
        val isNotifControls = AudioEffectManager.isNotifControlsEnabled.value
        val boost = AudioEffectManager.boostProgress.value
        val statusText = if (isEnabled) "Active (Boost: +$boost%)" else "Inactive (Booster Off)"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Milkys Sound Booster & EQ")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingTitleIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Milkys Sound Booster & EQ\nStatus: $statusText"))
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)

        if (isEnabled && isNotifControls) {
            builder.addAction(android.R.drawable.ic_media_previous, "-10%", pendingBoostDownIntent)
            builder.addAction(android.R.drawable.ic_media_next, "+10%", pendingBoostUpIntent)
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "OFF", pendingBoostOffIntent)
        } else {
            builder.addAction(
                if (isEnabled) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isEnabled) "Disable" else "Enable",
                pendingToggleIntent
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close", pendingStopIntent)
        }

        return builder.build()
    }

    private fun showFloatingOverlay() {
        if (overlayView != null) return
        
        // Ensure overlay permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            return
        }

        val typeFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            typeFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VolumeBoosterService)
            setViewTreeViewModelStoreOwner(this@VolumeBoosterService)
            setViewTreeSavedStateRegistryOwner(this@VolumeBoosterService)
            
            setContent {
                var isExpanded by remember { mutableStateOf(false) }
                val isBoosted by AudioEffectManager.isBoostEnabled.collectAsStateWithLifecycle()
                val boostProgress by AudioEffectManager.boostProgress.collectAsStateWithLifecycle()
                val currentPreset by AudioEffectManager.eqPreset.collectAsStateWithLifecycle()
                val favoritePresets by AudioEffectManager.favoritePresets.collectAsStateWithLifecycle()

                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    if (!isExpanded) {
                        // Collapsed Floating Bubble
                        FloatingBubble(
                            isBoosted = isBoosted,
                            boostProgress = boostProgress,
                            currentPreset = currentPreset,
                            onClick = { isExpanded = true },
                            onDrag = { dx, dy ->
                                overlayParams?.let { p ->
                                    p.x += dx.toInt()
                                    p.y += dy.toInt()
                                    
                                    val displayMetrics = resources.displayMetrics
                                    val screenWidth = displayMetrics.widthPixels
                                    val screenHeight = displayMetrics.heightPixels
                                    
                                    p.x = p.x.coerceIn(0, (screenWidth - 160).coerceAtLeast(0))
                                    p.y = p.y.coerceIn(0, (screenHeight - 160).coerceAtLeast(0))

                                    try {
                                        windowManager?.updateViewLayout(this@apply, p)
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                    }
                                }
                            },
                            onDragEnd = {
                                overlayParams?.let { p ->
                                    val displayMetrics = resources.displayMetrics
                                    val screenWidth = displayMetrics.widthPixels
                                    val midX = screenWidth / 2
                                    val targetX = if (p.x < midX) 16 else (screenWidth - 180).coerceAtLeast(16)
                                    
                                    p.x = targetX
                                    try {
                                        windowManager?.updateViewLayout(this@apply, p)
                                    } catch (e: Throwable) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        )
                    } else {
                        // Expanded Dashboard Control Over other apps
                        FloatingDashboard(
                            isBoosted = isBoosted,
                            boostProgress = boostProgress,
                            currentPreset = currentPreset,
                            favoritePresets = favoritePresets,
                            onToggleBoost = { AudioEffectManager.setBoostEnabled(it) },
                            onBoostChange = { AudioEffectManager.setBoostProgress(it) },
                            onPresetSelect = { AudioEffectManager.applyPreset(it) },
                            onOpenApp = {
                                try {
                                    val intent = Intent(this@VolumeBoosterService, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    startActivity(intent)
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }
                            },
                            onClose = { isExpanded = false },
                            onDisableOverlay = { AudioEffectManager.setFloatingEnabled(false) }
                        )
                    }
                }
            }
        }

        try {
            windowManager?.addView(overlayView, overlayParams)
        } catch (e: Throwable) {
            e.printStackTrace()
            overlayView = null
            overlayParams = null
        }
    }

    private fun hideFloatingOverlay() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        overlayView = null
        overlayParams = null
    }
}

fun getPresetAbbreviation(presetName: String): String {
    return when (presetName.lowercase()) {
        "flat" -> "FL"
        "bass booster" -> "BB"
        "vocal booster" -> "VB"
        "rock" -> "RK"
        "pop" -> "PO"
        "jazz" -> "JZ"
        else -> {
            val words = presetName.split(" ").filter { it.isNotBlank() }
            if (words.size >= 2) {
                "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
            } else if (presetName.length >= 2) {
                presetName.take(2).uppercase()
            } else {
                presetName.uppercase()
            }
        }
    }
}

fun getTopFavoritePresets(favorites: Set<String>): List<String> {
    val defaults = listOf("Flat", "Bass Booster", "Rock", "Pop")
    val result = mutableListOf<String>()
    for (fav in favorites) {
        if (result.size < 4 && !result.contains(fav)) {
            result.add(fav)
        }
    }
    for (def in defaults) {
        if (result.size < 4 && !result.contains(def)) {
            result.add(def)
        }
    }
    return result.take(4)
}

@Composable
fun FloatingBubble(
    isBoosted: Boolean,
    boostProgress: Int,
    currentPreset: String,
    onClick: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val modifierScale = if (isBoosted && boostProgress > 20) scale else 1.0f
    val presetAbbr = getPresetAbbreviation(currentPreset)

    Box(
        modifier = Modifier
            .scale(modifierScale)
            .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
            .size(60.dp)
            .pointerInput(Unit) {
                var totalDragDistance = 0f
                detectDragGestures(
                    onDragStart = { totalDragDistance = 0f },
                    onDragEnd = {
                        if (totalDragDistance < 15f) {
                            onClick()
                        } else {
                            onDragEnd()
                        }
                    },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragDistance += kotlin.math.hypot(dragAmount.x, dragAmount.y)
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            }
            .background(
                color = if (isBoosted) Color(0xFF381E72) else Color(0xFF2B2930),
                shape = CircleShape
            )
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Outer Power Ring Indicator
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = if (isBoosted) Color(0xFFD0BCFF) else Color(0xFF49454F)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isBoosted) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                    contentDescription = "Floating sound booster menu",
                    tint = if (isBoosted) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isBoosted) "+$boostProgress%" else presetAbbr,
                    color = if (isBoosted) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FloatingDashboard(
    isBoosted: Boolean,
    boostProgress: Int,
    currentPreset: String,
    favoritePresets: Set<String> = emptySet(),
    onToggleBoost: (Boolean) -> Unit,
    onBoostChange: (Int) -> Unit,
    onPresetSelect: (String) -> Unit = {},
    onOpenApp: () -> Unit = {},
    onClose: () -> Unit,
    onDisableOverlay: () -> Unit
) {
    val topFavorites = remember(favoritePresets) { getTopFavoritePresets(favoritePresets) }

    Card(
        modifier = Modifier
            .width(280.dp)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2B2930)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Power Switch & Action Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = "Active Booster",
                        tint = if (isBoosted) Color(0xFFD0BCFF) else Color(0xFFCAC4D0),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Booster Overlay",
                        color = Color(0xFFE6E1E5),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Open Main App Action
                    IconButton(
                        onClick = onOpenApp,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Open main app",
                            tint = Color(0xFFD0BCFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Disable Floating Overlay ('>')
                    IconButton(
                        onClick = onDisableOverlay,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Disable Overlay (>)",
                            tint = Color(0xFFCAC4D0),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Collapse Menu Action
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Collapse menu",
                            tint = Color(0xFFCAC4D0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Divider(color = Color(0xFF49454F), thickness = 1.dp)

            // Header / Power Switch Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Master Power State",
                    color = Color(0xFFCAC4D0),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Switch(
                    checked = isBoosted,
                    onCheckedChange = onToggleBoost,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFD0BCFF),
                        checkedTrackColor = Color(0xFF381E72),
                        uncheckedThumbColor = Color(0xFFCAC4D0),
                        uncheckedTrackColor = Color(0xFF49454F)
                    )
                )
            }

            // Boost Amplification Controls [-] / [+]
            val isSliderStepped by AudioEffectManager.isSliderStepped.collectAsStateWithLifecycle()
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Boost Amplification",
                        color = if (isBoosted) Color(0xFFE6E1E5) else Color(0xFFCAC4D0).copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "+$boostProgress%",
                        color = if (isBoosted) Color(0xFFD0BCFF) else Color(0xFFCAC4D0).copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Decrease Boost Button (-)
                    IconButton(
                        onClick = {
                            val step = if (isSliderStepped) 10 else 5
                            val newBoost = (boostProgress - step).coerceAtLeast(0)
                            onBoostChange(newBoost)
                        },
                        enabled = isBoosted && boostProgress > 0,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .size(48.dp)
                            .background(if (isBoosted) Color(0xFF49454F) else Color(0xFF36343B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease boost",
                            tint = if (isBoosted && boostProgress > 0) Color(0xFFD0BCFF) else Color(0xFFCAC4D0).copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Slider(
                        value = boostProgress.toFloat(),
                        onValueChange = { raw ->
                            val valInt = if (isSliderStepped) {
                                (kotlin.math.round(raw / 10f) * 10).toInt()
                            } else {
                                raw.toInt()
                            }
                            onBoostChange(valInt)
                        },
                        steps = if (isSliderStepped) 9 else 0,
                        valueRange = 0f..100f,
                        enabled = isBoosted,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFD0BCFF),
                            activeTrackColor = Color(0xFFD0BCFF),
                            inactiveTrackColor = Color(0xFF49454F)
                        )
                    )

                    // Increase Boost Button (+)
                    IconButton(
                        onClick = {
                            val step = if (isSliderStepped) 10 else 5
                            val newBoost = (boostProgress + step).coerceAtMost(100)
                            onBoostChange(newBoost)
                        },
                        enabled = isBoosted && boostProgress < 100,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .size(48.dp)
                            .background(if (isBoosted) Color(0xFF49454F) else Color(0xFF36343B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase boost",
                            tint = if (isBoosted && boostProgress < 100) Color(0xFFD0BCFF) else Color(0xFFCAC4D0).copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 4 Favorite Presets Grid / Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "FAVORITE PRESETS",
                    color = Color(0xFFCAC4D0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    topFavorites.forEach { presetName ->
                        val isSelected = currentPreset.equals(presetName, ignoreCase = true)
                        val abbr = getPresetAbbreviation(presetName)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    color = if (isSelected) Color(0xFFD0BCFF) else Color(0xFF36343B)
                                )
                                .clickable { onPresetSelect(presetName) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = abbr,
                                    color = if (isSelected) Color(0xFF381E72) else Color(0xFFE6E1E5),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = presetName,
                                    color = if (isSelected) Color(0xFF381E72) else Color(0xFFCAC4D0),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

