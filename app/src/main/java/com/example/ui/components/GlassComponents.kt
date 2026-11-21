package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FolderCopy
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackUiState
import com.example.data.db.RecordingEntity
import com.example.model.AudioFormatOption
import com.example.model.CallType
import com.example.ui.theme.CyberiTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.util.DeviceCompatibilityManager
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CyberGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = CyberiTheme.colors
    val auroraTransition = rememberInfiniteTransition(label = "aurora_drift")
    val drift by auroraTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    val orbAlpha1 = if (colors.isDark) 0.22f else 0.11f
    val orbAlpha2 = if (colors.isDark) 0.24f else 0.10f
    val orbAlpha3 = if (colors.isDark) 0.18f else 0.09f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(colors.bgTop, colors.bgBottom, colors.bgTop)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val aquaCenter = Offset(
                x = size.width * (0.78f + 0.08f * cos(drift)),
                y = size.height * (0.12f + 0.04f * sin(drift))
            )
            val violetCenter = Offset(
                x = size.width * (0.16f + 0.07f * sin(drift)),
                y = size.height * (0.48f + 0.05f * cos(drift))
            )
            val skyCenter = Offset(
                x = size.width * (0.80f - 0.06f * sin(drift)),
                y = size.height * (0.86f + 0.03f * cos(drift))
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.cyan.copy(alpha = orbAlpha1), Color.Transparent),
                    center = aquaCenter,
                    radius = size.width * 0.78f
                ),
                center = aquaCenter,
                radius = size.width * 0.78f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.indigo.copy(alpha = orbAlpha2), Color.Transparent),
                    center = violetCenter,
                    radius = size.width * 0.72f
                ),
                center = violetCenter,
                radius = size.width * 0.72f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors.blue.copy(alpha = orbAlpha3), Color.Transparent),
                    center = skyCenter,
                    radius = size.width * 0.65f
                ),
                center = skyCenter,
                radius = size.width * 0.65f
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    highlighted: Boolean = false,
    accentColor: Color = CyberiTheme.colors.cyan,
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CyberiTheme.colors
    val borderBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = if (colors.isDark) 0.85f else 0.70f),
                colors.indigo.copy(alpha = 0.45f),
                colors.glassBorderSubtle
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                if (colors.isDark) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.90f),
                colors.glassBorderSubtle,
                accentColor.copy(alpha = if (colors.isDark) 0.20f else 0.16f)
            )
        )
    }

    val fillBrush = Brush.linearGradient(
        colors = if (highlighted) {
            listOf(
                accentColor.copy(alpha = if (colors.isDark) 0.18f else 0.08f),
                colors.glassFillBottom
            )
        } else {
            listOf(
                colors.glassFillTop,
                colors.glassFillBottom
            )
        }
    )

    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .then(
                if (!colors.isDark) {
                    Modifier.shadow(
                        elevation = 6.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.07f),
                        spotColor = accentColor.copy(alpha = 0.12f)
                    )
                } else Modifier
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Column(
            modifier = Modifier
                .background(fillBrush)
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun StorageArcGauge(
    usedRatio: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = CyberiTheme.colors
    val animatedSweep by animateFloatAsState(
        targetValue = (usedRatio.coerceIn(0.03f, 1f)) * 270f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "storage_sweep"
    )
    Canvas(modifier = modifier) {
        val stroke = 7.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = colors.textMuted.copy(alpha = 0.22f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(colors.cyan, colors.indigo, accentColor)
            ),
            startAngle = 135f,
            sweepAngle = animatedSweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun LiveAmplitudeBars(
    amplitude: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = CyberiTheme.colors.crimson
) {
    val colors = CyberiTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val smoothAmp by animateFloatAsState(
        targetValue = if (isActive) (amplitude / 100f).coerceIn(0.14f, 1f) else 0.12f,
        animationSpec = tween(120),
        label = "amp"
    )

    Canvas(modifier = modifier.height(34.dp).fillMaxWidth()) {
        val barCount = 30
        val spacing = size.width / barCount
        val barWidth = (spacing * 0.52f).coerceAtLeast(3.dp.toPx())
        for (i in 0 until barCount) {
            val wave = if (isActive) {
                (0.45f + 0.55f * ((sin(phase + i * 0.55f) + 1f) / 2f)) * smoothAmp
            } else {
                0.22f + 0.18f * ((sin(i * 0.7f) + 1f) / 2f)
            }
            val barH = (size.height * wave).coerceAtLeast(4.dp.toPx())
            val x = i * spacing + (spacing - barWidth) / 2f
            val y = (size.height - barH) / 2f
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isActive) {
                        listOf(barColor, colors.indigo)
                    } else {
                        listOf(colors.cyan.copy(alpha = 0.5f), colors.indigo.copy(alpha = 0.28f))
                    }
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun WaveformBars(
    samplesCsv: String,
    progressFraction: Float,
    isPlaying: Boolean,
    accentColor: Color = CyberiTheme.colors.cyan,
    modifier: Modifier = Modifier
) {
    val colors = CyberiTheme.colors
    val bars = remember(samplesCsv) {
        val parsed = samplesCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .take(36)
        if (parsed.size >= 8) parsed else List(30) { idx ->
            24 + ((idx * 19) % 62)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val count = bars.size
        val step = size.width / count
        val barW = (step * 0.56f).coerceAtLeast(2.5.dp.toPx())
        for (i in 0 until count) {
            val ratio = (bars[i] / 100f).coerceIn(0.16f, 1f)
            val barH = size.height * ratio
            val x = i * step + (step - barW) / 2f
            val y = (size.height - barH) / 2f
            val barFraction = i.toFloat() / count
            val active = isPlaying && barFraction <= progressFraction
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (active) {
                        listOf(accentColor, colors.indigo)
                    } else {
                        listOf(colors.textSecondary.copy(alpha = 0.42f), colors.textMuted.copy(alpha = 0.22f))
                    }
                ),
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

@Composable
fun RecordingItemCard(
    recording: RecordingEntity,
    isPlayingThis: Boolean,
    playbackProgress: Float,
    onTogglePlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAssignContactClick: () -> Unit,
    onRenameClick: () -> Unit,
    onShareClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CyberiTheme.colors
    var menuExpanded by remember { mutableStateOf(false) }
    val callType = CallType.fromId(recording.callType)
    val format = AudioFormatOption.fromId(recording.formatId)

    val typeColor = when (callType) {
        CallType.INCOMING -> colors.emerald
        CallType.OUTGOING -> colors.blue
        CallType.MANUAL -> colors.amber
    }
    val typeIcon = when (callType) {
        CallType.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
        CallType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
        CallType.MANUAL -> Icons.Rounded.Mic
    }

    val playButtonColor by animateColorAsState(
        targetValue = if (isPlayingThis) colors.cyan else colors.surface,
        label = "play_bg"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recording_card_${recording.id}"),
        cornerRadius = 22.dp,
        highlighted = isPlayingThis,
        accentColor = if (isPlayingThis) colors.cyan else typeColor,
        contentPadding = 14.dp,
        onClick = onTogglePlay
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(typeColor, colors.indigo)
                        )
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("play_button_${recording.id}"),
                shape = CircleShape,
                color = playButtonColor,
                border = BorderStroke(
                    1.2.dp,
                    if (isPlayingThis) colors.cyan else typeColor.copy(alpha = 0.55f)
                ),
                onClick = onTogglePlay
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlayingThis) "توقف پخش" else "پخش مکالمه",
                        tint = if (isPlayingThis) MaterialTheme.colorScheme.onPrimary else typeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = callType.label,
                        tint = typeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = recording.contactName ?: recording.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                if (!recording.contactName.isNullOrBlank()) {
                    Text(
                        text = recording.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = format.extension.uppercase(),
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = DeviceCompatibilityManager.formatDuration(recording.durationMs),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "• ${DeviceCompatibilityManager.formatFileSize(recording.fileSizeBytes)}",
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("favorite_button_${recording.id}")
            ) {
                Icon(
                    imageVector = if (recording.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "نشان کردن مکالمه",
                    tint = if (recording.isFavorite) colors.crimson else colors.textSecondary
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("menu_button_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "گزینه‌های بیشتر",
                        tint = colors.textSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text("اتصال به مخاطب", color = colors.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.PersonAdd, null, tint = colors.indigo) },
                        onClick = {
                            menuExpanded = false
                            onAssignContactClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("تغییر نام", color = colors.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = colors.cyan) },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("اشتراک‌گذاری فایل", color = colors.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Share, null, tint = colors.emerald) },
                        onClick = {
                            menuExpanded = false
                            onShareClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ذخیره در پوشه Music", color = colors.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.FolderCopy, null, tint = colors.amber) },
                        onClick = {
                            menuExpanded = false
                            onExportClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف مکالمه", color = colors.crimson) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = colors.crimson) },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        WaveformBars(
            samplesCsv = recording.waveformSamples,
            progressFraction = playbackProgress,
            isPlaying = isPlayingThis,
            accentColor = typeColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = recording.audioSourceUsed,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = DeviceCompatibilityManager.formatTimestamp(recording.timestamp),
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }
    }
}

@Composable
fun MiniAudioPlayerBar(
    playbackState: PlaybackUiState,
    onTogglePlay: () -> Unit,
    onSeek: (Long) -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleBoost: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (playbackState.activeRecordingId == null) return

    val colors = CyberiTheme.colors
    val duration = playbackState.durationMs.coerceAtLeast(1L)
    val progress = (playbackState.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        cornerRadius = 22.dp,
        highlighted = true,
        accentColor = colors.cyan,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.testTag("mini_player_toggle")
            ) {
                Icon(
                    imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "پخش یا توقف",
                    tint = colors.cyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playbackState.activeTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = DeviceCompatibilityManager.formatDuration(playbackState.positionMs),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        color = colors.cyan
                    )
                    Text(
                        text = DeviceCompatibilityManager.formatDuration(duration),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (playbackState.isBoostEnabled) colors.emerald.copy(alpha = 0.20f) else colors.surface,
                border = BorderStroke(
                    1.dp,
                    if (playbackState.isBoostEnabled) colors.emerald else colors.glassBorderSubtle
                ),
                onClick = onToggleBoost,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mini_player_boost")
            ) {
                Text(
                    text = if (playbackState.isBoostEnabled) "+9dB" else "عادی",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = if (playbackState.isBoostEnabled) colors.emerald else colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.indigo.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, colors.indigo.copy(alpha = 0.5f)),
                onClick = onCycleSpeed,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("mini_player_speed")
            ) {
                Text(
                    text = "${playbackState.speed}x",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "بستن پخش‌کننده",
                    tint = colors.textSecondary
                )
            }
        }

        if (!playbackState.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = playbackState.errorMessage,
                style = MaterialTheme.typography.labelSmall,
                color = colors.amber,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Slider(
            value = progress,
            onValueChange = { fraction ->
                onSeek((fraction * duration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = colors.cyan,
                activeTrackColor = colors.cyan,
                inactiveTrackColor = colors.textMuted.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        )
    }
}
