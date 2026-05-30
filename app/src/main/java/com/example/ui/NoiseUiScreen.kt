package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.HourglassFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.NoisePlayer.NoiseType
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseUiScreen(
    viewModel: NoiseViewModel,
    modifier: Modifier = Modifier
) {
    val activeType by viewModel.activeType.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val whiteVol by viewModel.whiteVolume.collectAsState()
    val grayVol by viewModel.grayVolume.collectAsState()
    val pinkVol by viewModel.pinkVolume.collectAsState()
    val timerRemaining by viewModel.timerRemainingSeconds.collectAsState()

    // State controlling dialogs
    var showPresetsDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Bounce state for main toggle
    var isPressed by remember { mutableStateOf(false) }
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow),
        label = "bounce"
    )

    // Base immersive layout theme colors
    val canvasBg = Color(0xFF1A1C1E) // Immersive deep charcoal
    val cardHeaderColor = Color(0xFFC2C7CF) // Cool silver text

    // Dynamic color accents based on active mode
    val activeAccentColor = when (activeType) {
        NoiseType.WHITE -> Color(0xFF4DD0E1) // Bright aqua
        NoiseType.GRAY -> Color(0xFFCFD8DC)  // Sleek aluminum
        NoiseType.PINK -> Color(0xFFFFB1C8)  // Warm comfort rose pink
    }
    
    val activeLightColor = when (activeType) {
        NoiseType.WHITE -> Color(0xFFE0F7FA)
        NoiseType.GRAY -> Color(0xFFECEFF1)
        NoiseType.PINK -> Color(0xFFFFD9E2)
    }

    val activeQuietColor = when (activeType) {
        NoiseType.WHITE -> Color(0x334DD0E1)
        NoiseType.GRAY -> Color(0x33CFD8DC)
        NoiseType.PINK -> Color(0x33FFB1C8)
    }

    val activeContrastText = when (activeType) {
        NoiseType.PINK -> Color(0xFF31111D)
        NoiseType.WHITE -> Color(0xFF00363D)
        NoiseType.GRAY -> Color(0xFF263238)
    }

    val activeVolume = when (activeType) {
        NoiseType.WHITE -> whiteVol
        NoiseType.GRAY -> grayVol
        NoiseType.PINK -> pinkVol
    }

    // Interactive circular core pulsation
    val infiniteTransition = rememberInfiniteTransition(label = "corePulse")
    val waveScaleFactor by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val waveAlphaFactor by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = if (isPlaying) 0.35f else 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .pointerInput(Unit) {
                var dragAmountAccumulator = 0f
                detectVerticalDragGestures(
                    onDragStart = { dragAmountAccumulator = 0f },
                    onDragEnd = {
                        if (dragAmountAccumulator > 140f) {
                            // Swiped down -> Previous profile
                            viewModel.rotatePrevious()
                        } else if (dragAmountAccumulator < -140f) {
                            // Swiped up -> Next profile
                            viewModel.rotateNext()
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        dragAmountAccumulator += dragAmount
                    }
                )
            }
    ) {
        // Blur background aura
        Box(
            modifier = Modifier
                .size(310.dp)
                .align(Alignment.Center)
                .scale(waveScaleFactor)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            activeAccentColor.copy(alpha = waveAlphaFactor),
                            activeAccentColor.copy(alpha = waveAlphaFactor * 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Main Scaffold Equivalent arrangement
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Immersive Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NOISE STUDIO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.5.sp,
                        color = cardHeaderColor,
                        modifier = Modifier.testTag("app_identity_header")
                    )
                    if (timerRemaining != null) {
                        val minutesLeft = (timerRemaining ?: 0) / 60
                        val secondsLeft = (timerRemaining ?: 0) % 60
                        Text(
                            text = "TIMER ACTIVE: ${String.format("%02d:%02d", minutesLeft, secondsLeft)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            color = activeAccentColor
                        )
                    }
                }

                // Cog settings button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF303033))
                        .clickable { showInfoDialog = true }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Acoustic Information Settings",
                        tint = Color(0xFFC4C6D0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Playback Core Bubble
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Circle trigger button matching exact specs
                    Box(
                        modifier = Modifier
                            .size(208.dp) // Tailwind w-52
                            .scale(playButtonScale)
                            .clip(CircleShape)
                            .background(activeAccentColor)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                        viewModel.togglePlayPause()
                                    }
                                )
                            }
                            .testTag("core_interactive_bubble"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Play/Pause icon inside
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop Playback" else "Start Playback",
                            tint = activeContrastText,
                            modifier = Modifier.size(68.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    // Title Header: Pink Noise, White Noise, Gray Noise
                    Text(
                        text = when (activeType) {
                            NoiseType.WHITE -> "White Noise"
                            NoiseType.GRAY -> "Gray Noise"
                            NoiseType.PINK -> "Pink Noise"
                        },
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp,
                        color = activeAccentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("active_noise_title")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Cozy descriptive caption
                    Text(
                        text = when (activeType) {
                            NoiseType.PINK -> "Deep, soothing low frequencies for focus and restful sleep."
                            NoiseType.WHITE -> "Infinite full-spectrum sharp masking to target ambient noise."
                            NoiseType.GRAY -> "Equal-loudness compensated curve for psychoacoustic calm."
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = cardHeaderColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Flowing cozy mini visualizer waves
                    WaveformVisualizer(isPlaying = isPlaying, activeColor = activeAccentColor)
                }
            }

            // 3. Immersive Intensity Sliders Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                // Label row with active percentage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTENSITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.8.sp,
                        color = activeAccentColor
                    )
                    Text(
                        text = "${(activeVolume * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = cardHeaderColor
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Custom High-Fidelity Retro Thick Slider
                ImmersiveSlider(
                    value = activeVolume,
                    onValueChange = {
                        when (activeType) {
                            NoiseType.WHITE -> viewModel.setWhiteVolume(it)
                            NoiseType.GRAY -> viewModel.setGrayVolume(it)
                            NoiseType.PINK -> viewModel.setPinkVolume(it)
                        }
                    },
                    accentColor = activeAccentColor,
                    thumbColor = activeLightColor,
                    borderColor = activeContrastText
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Dot Indicator rotation pills matching specifications
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoiseType.values().forEachIndexed { index, type ->
                        val isActive = activeType == type
                        val pillWidth by animateDpAsState(targetValue = if (isActive) 24.dp else 8.dp, label = "pill")
                        val pillColor = if (isActive) activeAccentColor else Color(0xFF45474A)

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = pillWidth, height = 8.dp)
                                .clip(CircleShape)
                                .background(pillColor)
                                .clickable { viewModel.changeType(type) }
                        )
                    }
                }
            }

            // 5. Aesthetic Action Shelf Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2A2D30))
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sub action: Presets Dialog
                    Column(
                        modifier = Modifier
                            .clickable { showPresetsDialog = true }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Presets Mixer Menu",
                            tint = Color(0xFFC2C7CF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Presets", fontSize = 10.sp, color = Color(0xFFC2C7CF))
                    }

                    // Main active tracker display center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) activeAccentColor else Color(0xFF45474A))
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isPlaying) "Now Playing" else "Paused",
                            fontSize = 10.sp,
                            color = activeAccentColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Sub action: Timer trigger
                    Column(
                        modifier = Modifier
                            .clickable { showTimerDialog = true }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (timerRemaining != null) Icons.Default.HourglassFull else Icons.Default.HourglassEmpty,
                            contentDescription = "Sleep Countdown Timer Toggle",
                            tint = if (timerRemaining != null) activeAccentColor else Color(0xFFC2C7CF),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Timer", fontSize = 10.sp, color = Color(0xFFC2C7CF))
                    }
                }
            }
        }

        // TRANSITIONS & SYSTEM DIALOGS
        
        // Settings / Info dialog
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                icon = { Icon(Icons.Default.Info, contentDescription = null) },
                title = { Text("Acoustic Information") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Acoustic spectrum details of your NoiseScape profiles:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Text("White Noise", fontWeight = FontWeight.Bold, color = Color(0xFF4DD0E1))
                        Text("Emits equal power across all audible frequencies. Perfect for blocking sudden traffic noises or distracting chatter.", fontSize = 13.sp)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Gray Noise", fontWeight = FontWeight.Bold, color = Color(0xFFCFD8DC))
                        Text("Emphasizes both low and high frequencies, matching the human ear's equal-loudness contours for an exceptionally balanced sound.", fontSize = 13.sp)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text("Pink Noise", fontWeight = FontWeight.Bold, color = Color(0xFFFFB1C8))
                        Text("Power decreases as frequency increases, creating a deeper, richer hum that mimics ocean waves or heavy rainfall.", fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfoDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Sleep timer setting Dialog
        if (showTimerDialog) {
            AlertDialog(
                onDismissRequest = { showTimerDialog = false },
                title = { Text("Sleep Timer Configuration") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (timerRemaining != null) "A timer is currently active. Would you like to stop it or reset the duration?" else "Select countdown duration:",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    viewModel.startSleepTimer(15)
                                    showTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303033))
                            ) {
                                Text("15 M", color = Color.White)
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.startSleepTimer(30)
                                    showTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303033))
                            ) {
                                Text("30 M", color = Color.White)
                            }
                            
                            Button(
                                onClick = {
                                    viewModel.startSleepTimer(60)
                                    showTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF303033))
                            ) {
                                Text("60 M", color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {
                    if (timerRemaining != null) {
                        TextButton(onClick = {
                            viewModel.cancelSleepTimer()
                            showTimerDialog = false
                        }) {
                            Text("Turn Off Timer", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Ambient Presets mixer Sheet
        if (showPresetsDialog) {
            AlertDialog(
                onDismissRequest = { showPresetsDialog = false },
                title = { Text("Acoustic Presets") },
                text = {
                    Column {
                        Text(
                            text = "Apply preset mixes instantly adjusted for specific environments:",
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        PresetRowItem(title = "Deep Sleep Sanctuary", desc = "Soft warm pink frequencies", color = Color(0xFFFFB1C8)) {
                            viewModel.applyPreset("Sleep")
                            showPresetsDialog = false
                        }
                        
                        PresetRowItem(title = "Study & Focus Block", desc = "High power sharp masking", color = Color(0xFF4DD0E1)) {
                            viewModel.applyPreset("Focus")
                            showPresetsDialog = false
                        }
                        
                        PresetRowItem(title = "Balanced Ocean Roar", desc = "Quiet psych-acoustic comfort", color = Color(0xFFCFD8DC)) {
                            viewModel.applyPreset("Calm")
                            showPresetsDialog = false
                        }

                        PresetRowItem(title = "Silence All", desc = "Mute mixer completely", color = Color.Gray) {
                            viewModel.applyPreset("Mute")
                            showPresetsDialog = false
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPresetsDialog = false }) {
                        Text("Back")
                    }
                }
            )
        }
    }
}

/**
 * Highly responsive custom draggable slider mimicking retro-cozy slider in specs
 */
@Composable
fun ImmersiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color,
    thumbColor: Color,
    borderColor: Color
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(onValueChange) {
                detectTapGestures(
                    onPress = { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(fraction)
                    }
                )
            }
    ) {
        val maxW = this.maxWidth
        val totalWidthPx = constraints.maxWidth.toFloat()
        
        // Horizontal gesture tracking
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(totalWidthPx, onValueChange) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val currentPx = value * totalWidthPx
                            val nextPx = currentPx + dragAmount
                            val fraction = (nextPx / totalWidthPx).coerceIn(0f, 1f)
                            onValueChange(fraction)
                        }
                    )
                },
            contentAlignment = Alignment.CenterStart
        ) {
            // Track background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF45474A))
            )

            // Active track level color of the active profile
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor)
            )

            // Calculated offset inside container safely
            val thumbMaxOffsetDp = maxW - 40.dp
            val thumbOffsetDp = (value * thumbMaxOffsetDp.value).dp.coerceAtLeast(0.dp)
            
            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetDp)
                    .size(40.dp)
                    .border(4.dp, borderColor, CircleShape)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}

/**
 * Procedural minimalist waveform visualization
 */
@Composable
fun WaveformVisualizer(
    isPlaying: Boolean,
    activeColor: Color
) {
    val transition = rememberInfiniteTransition(label = "waveAnim")
    
    // Wave animation phase parameter
    val animPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * java.lang.Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val ampScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.04f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ampScale"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .padding(horizontal = 40.dp)
    ) {
        val width = size.width
        val height = size.height
        val points = 60
        val dx = width / points

        // Draw multiple offset sine curves to represent thick, cozy noise waves
        for (waveIndex in 0..2) {
            val scale = when (waveIndex) {
                0 -> 1.0f
                1 -> 0.6f
                else -> 0.3f
            }
            val speedFactor = when (waveIndex) {
                0 -> 1.0f
                1 -> -1.3f
                else -> 0.7f
            }
            val pathColor = activeColor.copy(
                alpha = when (waveIndex) {
                    0 -> 0.7f
                    1 -> 0.4f
                    else -> 0.2f
                } * ampScale
            )

            for (i in 0 until points) {
                val x1 = i * dx
                val x2 = (i + 1) * dx

                val angle1 = (i.toFloat() / points.toFloat()) * 4f * java.lang.Math.PI.toFloat() + (animPhase * speedFactor)
                val angle2 = ((i + 1).toFloat() / points.toFloat()) * 4f * java.lang.Math.PI.toFloat() + (animPhase * speedFactor)

                val y1 = (height / 2) + sin(angle1) * (height / 3) * scale * ampScale
                val y2 = (height / 2) + sin(angle2) * (height / 3) * scale * ampScale

                drawLine(
                    color = pathColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }
    }
}

@Composable
fun PresetRowItem(
    title: String,
    desc: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF303033))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Text(desc, fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

// Add horizontal drag detector interface for custom slider
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectHorizontalDragGestures(
    onHorizontalDrag: (change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Float) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false)
            var drag: androidx.compose.ui.input.pointer.PointerInputChange? = down
            while (drag != null && drag.pressed) {
                val event = awaitPointerEvent()
                drag = event.changes.firstOrNull { it.id == down.id }
                if (drag != null && drag.pressed) {
                    val dragAmount = drag.position.x - drag.previousPosition.x
                    if (dragAmount != 0f) {
                        onHorizontalDrag(drag, dragAmount)
                    }
                }
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.awaitFirstDown(
    requireUnconsumed: Boolean
): androidx.compose.ui.input.pointer.PointerInputChange {
    while (true) {
        val event = awaitPointerEvent()
        val firstDown = event.changes.firstOrNull {
            if (requireUnconsumed) !it.isConsumed else true
        }
        if (firstDown != null && firstDown.pressed) {
            return firstDown
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.forEachGesture(
    block: suspend androidx.compose.ui.input.pointer.PointerInputScope.() -> Unit
) {
    while (true) {
        try {
            block()
        } catch (e: Exception) {
            // ignore and retry
        }
    }
}
