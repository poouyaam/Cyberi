package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberDeepNavy
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.GlassBorderBright
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassFill
import com.example.ui.theme.GlassFillLight
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DeviceCompatibilityManager
import kotlin.math.sin

@Composable
fun CyberGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CyberVoid, CyberDeepNavy, CyberVoid)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyberCyan.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.10f),
                    radius = size.width * 0.72f
                ),
                center = Offset(size.width * 0.85f, size.height * 0.10f),
                radius = size.width * 0.72f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyberIndigo.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.55f),
                    radius = size.width * 0.65f
                ),
                center = Offset(size.width * 0.12f, size.height * 0.55f),
                radius = size.width * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(CyberBlue.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.75f, size.height * 0.90f),
                    radius = size.width * 0.55f
                ),
                center = Offset(size.width * 0.75f, size.height * 0.90f),
                radius = size.width * 0.55f
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
    accentColor: Color = CyberCyan,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = if (highlighted) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.7f),
                GlassBorderSubtle,
                accentColor.copy(alpha = 0.35f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                GlassBorderBright.copy(alpha = 0.35f),
                GlassBorderSubtle,
                Color.White.copy(alpha = 0.08f)
            )
        )
    }

    val fillBrush = Brush.linearGradient(
        colors = if (highlighted) {
            listOf(
                accentColor.copy(alpha = 0.14f),
                GlassFill
            )
        } else {
            listOf(
                GlassFillLight,
                GlassFill
            )
        }
    )

    val shape = RoundedCornerShape(cornerRadius)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        Column(
            modifier = Modifier
                .background(fillBrush)
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun LiveAmplitudeBars(
    amplitude: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = CyberCrimson
) {
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
        targetValue = if (isActive) (amplitude / 100f).coerceIn(0.12f, 1f) else 0.08f,
        animationSpec = tween(120),
        label = "amp"
    )

    Canvas(modifier = modifier.height(34.dp).fillMaxWidth()) {
        val barCount = 28
        val spacing = size.width / barCount
        val barWidth = (spacing * 0.55f).coerceAtLeast(3.dp.toPx())
        for (i in 0 until barCount) {
            val wave = if (isActive) {
                (0.45f + 0.55f * ((sin(phase + i * 0.55f) + 1f) / 2f)) * smoothAmp
            } else {
                0.15f
            }
            val barH = (size.height * wave).coerceAtLeast(4.dp.toPx())
            val x = i * spacing + (spacing - barWidth) / 2f
            val y = (size.height - barH) / 2f
            drawRoundRect(
                color = if (isActive) barColor else TextMuted.copy(alpha = 0.4f),
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
    modifier: Modifier = Modifier
) {
    val bars = remember(samplesCsv) {
        val parsed = samplesCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .take(36)
        if (parsed.size >= 8) parsed else List(30) { idx ->
            22 + ((idx * 17) % 58)
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
    ) {
        val count = bars.size
        val step = size.width / count
        val barW = (step * 0.58f).coerceAtLeast(2.5.dp.toPx())
        for (i in 0 until count) {
            val ratio = (bars[i] / 100f).coerceIn(0.14f, 1f)
            val barH = size.height * ratio
            val x = i * step + (step - barW) / 2f
            val y = (size.height - barH) / 2f
            val barFraction = i.toFloat() / count
            val active = isPlaying && barFraction <= progressFraction
            drawRoundRect(
                color = if (active) CyberCyan else TextSecondary.copy(alpha = 0.35f),
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
    onRenameClick: () -> Unit,
    onShareClick: () -> Unit,
    onExportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val callType = CallType.fromId(recording.callType)
    val format = AudioFormatOption.fromId(recording.formatId)

    val typeColor = when (callType) {
        CallType.INCOMING -> CyberEmerald
        CallType.OUTGOING -> CyberCyan
        CallType.MANUAL -> CyberAmber
    }
    val typeIcon = when (callType) {
        CallType.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
        CallType.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
        CallType.MANUAL -> Icons.Rounded.Mic
    }

    val playButtonColor by animateColorAsState(
        targetValue = if (isPlayingThis) CyberCyan else CyberSurface,
        label = "play_bg"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recording_card_${recording.id}"),
        cornerRadius = 22.dp,
        highlighted = isPlayingThis,
        accentColor = CyberCyan,
        onClick = onTogglePlay
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play / Pause Circular Glass Button
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("play_button_${recording.id}"),
                shape = CircleShape,
                color = playButtonColor,
                border = BorderStroke(
                    1.dp,
                    if (isPlayingThis) CyberCyan else GlassBorderBright.copy(alpha = 0.45f)
                ),
                onClick = onTogglePlay
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlayingThis) "توقف پخش" else "پخش مکالمه",
                        tint = if (isPlayingThis) CyberVoid else CyberCyan,
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
                        text = recording.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberCyan.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = format.extension.uppercase(),
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = DeviceCompatibilityManager.formatDuration(recording.durationMs),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "• ${DeviceCompatibilityManager.formatFileSize(recording.fileSizeBytes)}",
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
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
                    tint = if (recording.isFavorite) CyberCrimson else TextSecondary
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
                        tint = TextSecondary
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = CyberDeepNavy
                ) {
                    DropdownMenuItem(
                        text = { Text("تغییر نام", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = CyberCyan) },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("اشتراک‌گذاری فایل", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Share, null, tint = CyberEmerald) },
                        onClick = {
                            menuExpanded = false
                            onShareClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ذخیره در پوشه Music", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.FolderCopy, null, tint = CyberAmber) },
                        onClick = {
                            menuExpanded = false
                            onExportClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("حذف مکالمه", color = CyberCrimson) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = CyberCrimson) },
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
            isPlaying = isPlayingThis
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
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = DeviceCompatibilityManager.formatTimestamp(recording.timestamp),
                fontFamily = SpaceGroteskFontFamily,
                fontSize = 11.sp,
                color = TextMuted
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
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (playbackState.activeRecordingId == null) return

    val duration = playbackState.durationMs.coerceAtLeast(1L)
    val progress = (playbackState.positionMs.toFloat() / duration).coerceIn(0f, 1f)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        cornerRadius = 20.dp,
        highlighted = true,
        accentColor = CyberCyan
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
                    tint = CyberCyan,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playbackState.activeTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
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
                        color = CyberCyan
                    )
                    Text(
                        text = DeviceCompatibilityManager.formatDuration(duration),
                        fontFamily = SpaceGroteskFontFamily,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = CyberCyan.copy(alpha = 0.16f),
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
                    color = CyberCyan,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "بستن پخش‌کننده",
                    tint = TextSecondary
                )
            }
        }

        Slider(
            value = progress,
            onValueChange = { fraction ->
                onSeek((fraction * duration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = CyberCyan,
                activeTrackColor = CyberCyan,
                inactiveTrackColor = TextMuted.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
        )
    }
}
