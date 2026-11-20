package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackUiState
import com.example.data.db.RecordingEntity
import com.example.model.RecordingSettings
import com.example.service.LiveRecorderState
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveAmplitudeBars
import com.example.ui.components.RecordingItemCard
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCrimson
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DeviceCompatibilityManager
import com.example.util.DeviceCompatibilityStatus

@Composable
fun HomeScreen(
    settings: RecordingSettings,
    liveState: LiveRecorderState,
    deviceStatus: DeviceCompatibilityStatus,
    recentRecordings: List<RecordingEntity>,
    playbackState: PlaybackUiState,
    onRequestPermissions: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleAutoRecord: () -> Unit,
    onToggleManualRecord: () -> Unit,
    onTogglePlay: (RecordingEntity) -> Unit,
    onToggleFavorite: (RecordingEntity) -> Unit,
    onRenameRecording: (Long, String) -> Unit,
    onShareRecording: (RecordingEntity) -> Unit,
    onExportRecording: (RecordingEntity) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onOpenAllRecordings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var renameTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<RecordingEntity?>(null) }

    val pulseTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Brand Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CyberCyan.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneInTalk,
                                    contentDescription = "سایبری",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "سایبری",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "ضبط هوشمند مکالمات تلفنی",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }

                    // Compact status badge
                    val badgeColor = when {
                        liveState.isRecording -> CyberCrimson
                        !deviceStatus.allCorePermissionsGranted -> CyberAmber
                        settings.autoRecordEnabled -> CyberEmerald
                        else -> TextMuted
                    }
                    val badgeText = when {
                        liveState.isRecording -> "در حال ضبط"
                        !deviceStatus.allCorePermissionsGranted -> "نیاز به مجوز"
                        settings.autoRecordEnabled -> "آماده و فعال"
                        else -> "متوقف"
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = badgeColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = badgeColor,
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelLarge,
                                color = badgeColor
                            )
                        }
                    }
                }
            }

            // Smart Permission & Device Sync Card (shown when any core permission or battery setup needs attention)
            if (!deviceStatus.allRecommendedReady || !deviceStatus.isBatteryOptimizationIgnored) {
                item {
                    GlassCard(
                        highlighted = !deviceStatus.allCorePermissionsGranted,
                        accentColor = if (!deviceStatus.allCorePermissionsGranted) CyberAmber else CyberCyan
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (deviceStatus.allCorePermissionsGranted) {
                                    Icons.Rounded.BatteryChargingFull
                                } else {
                                    Icons.Rounded.Security
                                },
                                contentDescription = null,
                                tint = if (deviceStatus.allCorePermissionsGranted) CyberCyan else CyberAmber,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!deviceStatus.allCorePermissionsGranted) {
                                        "هماهنگ‌سازی هوشمند با گوشی"
                                    } else {
                                        "بهینه‌سازی عملکرد پس‌زمینه"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (!deviceStatus.allCorePermissionsGranted) {
                                        "برای شروع و پایان خودکار ضبط هنگام تماس، یک‌بار مجوزها را تایید کنید."
                                    } else {
                                        "برای جلوگیری از بسته شدن سرویس در پس‌زمینه، سایبری را از محدودیت باتری خارج کنید."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!deviceStatus.allRecommendedReady) {
                                Button(
                                    onClick = onRequestPermissions,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyberAmber,
                                        contentColor = CyberVoid
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("grant_permissions_button")
                                ) {
                                    Text("تایید خودکار دسترسی‌ها", fontWeight = FontWeight.Bold)
                                }
                            }
                            if (!deviceStatus.isBatteryOptimizationIgnored) {
                                OutlinedButton(
                                    onClick = onOpenBatterySettings,
                                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("battery_opt_button")
                                ) {
                                    Text("تنظیم باتری گوشی", color = CyberCyan)
                                }
                            }
                        }
                    }
                }
            }

            // Hero Master Control Card (Simple 1-Tap ON/OFF)
            item {
                val isAutoActive = settings.autoRecordEnabled && deviceStatus.allCorePermissionsGranted
                val orbColor = when {
                    liveState.isRecording -> CyberCrimson
                    isAutoActive -> CyberCyan
                    else -> TextMuted
                }

                GlassCard(
                    highlighted = isAutoActive || liveState.isRecording,
                    accentColor = orbColor
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Big Tactile Orb Button
                        Surface(
                            modifier = Modifier
                                .size(124.dp)
                                .scale(if (isAutoActive || liveState.isRecording) pulseScale else 1f)
                                .testTag("master_auto_toggle"),
                            shape = CircleShape,
                            color = orbColor.copy(alpha = 0.16f),
                            border = BorderStroke(2.5.dp, orbColor),
                            onClick = {
                                if (!deviceStatus.allCorePermissionsGranted) {
                                    onRequestPermissions()
                                } else {
                                    onToggleAutoRecord()
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = if (liveState.isRecording) {
                                            Icons.Rounded.GraphicEq
                                        } else {
                                            Icons.Rounded.PowerSettingsNew
                                        },
                                        contentDescription = "تغییر وضعیت ضبط خودکار",
                                        tint = orbColor,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isAutoActive) "روشن" else "خاموش",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = orbColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = when {
                                !deviceStatus.allCorePermissionsGranted -> "برای فعال‌سازی دایره بالا را لمس کنید"
                                settings.autoRecordEnabled -> "ضبط خودکار همه مکالمات فعال است"
                                else -> "ضبط خودکار تماس‌ها موقتاً خاموش است"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "به محض پاسخ به تماس ورودی یا شروع تماس خروجی، ضبط با وضوح بالا آغاز و در پایان ذخیره می‌شود.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Active Configuration Summary Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoGlassPill(
                                label = "فرمت اصلی: ${settings.audioFormat.extension.uppercase()}",
                                icon = Icons.Rounded.CheckCircle,
                                tint = CyberCyan,
                                modifier = Modifier.weight(1f),
                                onClick = onOpenSettings
                            )
                            InfoGlassPill(
                                label = "${settings.sampleRateHz / 1000}kHz • ${settings.bitrateKbps}kbps",
                                icon = Icons.Rounded.Tune,
                                tint = CyberEmerald,
                                modifier = Modifier.weight(1f),
                                onClick = onOpenSettings
                            )
                        }
                    }
                }
            }

            // Live Recording Status OR Instant Test Record Card
            item {
                GlassCard(
                    highlighted = liveState.isRecording,
                    accentColor = CyberCrimson
                ) {
                    AnimatedVisibility(visible = liveState.isRecording) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = CyberCrimson,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Text(
                                        text = "در حال ضبط (${liveState.currentCallType?.label ?: "مکالمه"})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = CyberCrimson
                                    )
                                }
                                Text(
                                    text = DeviceCompatibilityManager.formatDuration(liveState.elapsedMs),
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = TextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LiveAmplitudeBars(
                                amplitude = liveState.amplitude,
                                isActive = true,
                                barColor = CyberCrimson
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "مسیر: ${liveState.activeSourceLabel} • فرمت: ${liveState.activeFormat.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = onToggleManualRecord,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCrimson,
                                    contentColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("stop_recording_button")
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("پایان ضبط و ذخیره فایل", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    AnimatedVisibility(visible = !liveState.isRecording) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تست سریع کیفیت صدا",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "پیش از تماس، وضوح ضبط با فرمت ${settings.audioFormat.extension.uppercase()} را آزمایش کنید",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (!deviceStatus.hasRecordAudio) {
                                        onRequestPermissions()
                                    } else {
                                        onToggleManualRecord()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyberCyan,
                                    contentColor = CyberVoid
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("manual_record_button")
                            ) {
                                Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ضبط تست", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!liveState.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = CyberAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = liveState.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CyberAmber
                            )
                        }
                    }
                }
            }

            // Recent Recordings Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آخرین مکالمات ضبط‌شده",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    if (recentRecordings.isNotEmpty()) {
                        TextButton(
                            onClick = onOpenAllRecordings,
                            modifier = Modifier.testTag("see_all_recordings_button")
                        ) {
                            Text("مشاهده همه", color = CyberCyan)
                        }
                    }
                }
            }

            if (recentRecordings.isEmpty()) {
                item {
                    GlassCard {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = CyberCyan.copy(alpha = 0.65f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "هنوز مکالمه‌ای ضبط نشده است",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "با اولین تماس ورودی یا خروجی، فایل صوتی به‌صورت خودکار اینجا نمایش داده می‌شود.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recentRecordings.take(4), key = { it.id }) { recording ->
                    val isPlayingThis = playbackState.activeRecordingId == recording.id && playbackState.isPlaying
                    val progress = if (playbackState.activeRecordingId == recording.id && playbackState.durationMs > 0) {
                        playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()
                    } else 0f

                    RecordingItemCard(
                        recording = recording,
                        isPlayingThis = isPlayingThis,
                        playbackProgress = progress,
                        onTogglePlay = { onTogglePlay(recording) },
                        onToggleFavorite = { onToggleFavorite(recording) },
                        onRenameClick = { renameTarget = recording },
                        onShareClick = { onShareRecording(recording) },
                        onExportClick = { onExportRecording(recording) },
                        onDeleteClick = { deleteTarget = recording }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    renameTarget?.let { target ->
        RenameRecordingDialog(
            currentTitle = target.title,
            onDismiss = { renameTarget = null },
            onConfirm = { newTitle ->
                onRenameRecording(target.id, newTitle)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        DeleteRecordingDialog(
            title = target.title,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeleteRecording(target)
                deleteTarget = null
            }
        )
    }
}

@Composable
private fun InfoGlassPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = CyberSurface.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1
            )
        }
    }
}
