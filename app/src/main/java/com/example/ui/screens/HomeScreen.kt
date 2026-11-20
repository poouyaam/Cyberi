package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackUiState
import com.example.data.db.RecordingEntity
import com.example.data.repository.DeviceContact
import com.example.model.RecordingSettings
import com.example.service.LiveRecorderState
import com.example.service.PermissionSummary
import com.example.ui.components.GlassCard
import com.example.ui.components.LiveAmplitudeBars
import com.example.ui.components.RecordingItemCard
import com.example.ui.components.StorageArcGauge
import com.example.ui.theme.CyberiTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.util.DeviceCompatibilityManager

@Composable
fun HomeScreen(
    settings: RecordingSettings,
    liveState: LiveRecorderState,
    permissionSummary: PermissionSummary,
    totalStorageBytes: Long,
    recentRecordings: List<RecordingEntity>,
    contacts: List<DeviceContact>,
    playbackState: PlaybackUiState,
    onRequestPermissions: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleAutoRecord: () -> Unit,
    onToggleManualRecord: () -> Unit,
    onShareApp: () -> Unit,
    onTogglePlay: (RecordingEntity) -> Unit,
    onToggleFavorite: (RecordingEntity) -> Unit,
    onLoadContacts: (String) -> Unit,
    onAssignContact: (Long, String?) -> Unit,
    onRenameRecording: (Long, String) -> Unit,
    onShareRecording: (RecordingEntity) -> Unit,
    onExportRecording: (RecordingEntity) -> Unit,
    onDeleteRecording: (RecordingEntity) -> Unit,
    onOpenAllRecordings: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CyberiTheme.colors
    var contactTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var renameTarget by remember { mutableStateOf<RecordingEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<RecordingEntity?>(null) }

    val pulseTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val radarRotation by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_rot"
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Floating Glass Brand Header
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(colors.cyan.copy(alpha = 0.20f), colors.indigo.copy(alpha = 0.25f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneInTalk,
                                contentDescription = "سایبری",
                                tint = colors.cyan,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "سایبری",
                                style = MaterialTheme.typography.headlineMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "ضبط هوشمند و خودکار مکالمات",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onShareApp,
                            modifier = Modifier.testTag("home_share_app_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "اشتراک‌گذاری برنامه سایبری",
                                tint = colors.cyan
                            )
                        }

                        val badgeColor = when {
                            liveState.isRecording -> colors.crimson
                            !permissionSummary.allEssentialGranted -> colors.amber
                            settings.autoRecordEnabled -> colors.emerald
                            else -> colors.textMuted
                        }
                        val badgeText = when {
                            liveState.isRecording -> "در حال ضبط"
                            !permissionSummary.allEssentialGranted -> "${permissionSummary.grantedCount}/${permissionSummary.totalCount} مجوز"
                            settings.autoRecordEnabled -> "آماده و فعال"
                            else -> "متوقف"
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = badgeColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
                            onClick = {
                                if (!permissionSummary.allFullAutomationGranted) onRequestPermissions()
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
            }

            // Smart Permission & Automation Banner
            if (!permissionSummary.allFullAutomationGranted || !permissionSummary.isBatteryOptimizationIgnored) {
                item {
                    GlassCard(
                        highlighted = !permissionSummary.allEssentialGranted,
                        accentColor = if (!permissionSummary.allEssentialGranted) colors.amber else colors.indigo
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (permissionSummary.allEssentialGranted) {
                                    Icons.Rounded.BatteryChargingFull
                                } else {
                                    Icons.Rounded.Security
                                },
                                contentDescription = null,
                                tint = if (permissionSummary.allEssentialGranted) colors.indigo else colors.amber,
                                modifier = Modifier.size(26.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (!permissionSummary.allFullAutomationGranted) {
                                        "سرویس دسترسی خودکار (${permissionSummary.grantedCount} از ${permissionSummary.totalCount})"
                                    } else {
                                        "بهینه‌سازی باتری پس‌زمینه"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = if (!permissionSummary.allFullAutomationGranted) {
                                        "برای اتوماسیون کامل تماس‌ها و تطبیق نام مخاطبین، دسترسی‌های لازم را تایید کنید."
                                    } else {
                                        "برای فعال ماندن همیشگی ضبط خودکار، سایبری را از محدودیت باتری خارج کنید."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!permissionSummary.allFullAutomationGranted) {
                                Button(
                                    onClick = onRequestPermissions,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.amber,
                                        contentColor = Color.White
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
                            if (!permissionSummary.isBatteryOptimizationIgnored) {
                                OutlinedButton(
                                    onClick = onOpenBatterySettings,
                                    border = BorderStroke(1.dp, colors.cyan.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("battery_opt_button")
                                ) {
                                    Text("تنظیم باتری", color = colors.cyan)
                                }
                            }
                        }
                    }
                }
            }

            // Hero Radar Orb Control Card
            item {
                val isAutoActive = settings.autoRecordEnabled && permissionSummary.allEssentialGranted
                val orbColor = when {
                    liveState.isRecording -> colors.crimson
                    isAutoActive -> colors.cyan
                    else -> colors.textMuted
                }

                GlassCard(
                    highlighted = isAutoActive || liveState.isRecording,
                    accentColor = orbColor
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier.size(144.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 3.dp.toPx()
                                if (isAutoActive || liveState.isRecording) {
                                    rotate(radarRotation) {
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    orbColor.copy(alpha = 0.85f),
                                                    colors.indigo.copy(alpha = 0.7f),
                                                    Color.Transparent
                                                )
                                            ),
                                            startAngle = 0f,
                                            sweepAngle = 220f,
                                            useCenter = false,
                                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                                        )
                                    }
                                } else {
                                    drawCircle(
                                        color = colors.textMuted.copy(alpha = 0.25f),
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .size(116.dp)
                                    .scale(if (isAutoActive || liveState.isRecording) pulseScale else 1f)
                                    .testTag("master_auto_toggle"),
                                shape = CircleShape,
                                color = orbColor.copy(alpha = 0.14f),
                                border = BorderStroke(2.dp, orbColor.copy(alpha = 0.85f)),
                                onClick = {
                                    if (!permissionSummary.allEssentialGranted) {
                                        onRequestPermissions()
                                    } else {
                                        onToggleAutoRecord()
                                    }
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    orbColor.copy(alpha = 0.22f),
                                                    Color.Transparent
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = if (liveState.isRecording) {
                                                Icons.Rounded.GraphicEq
                                            } else {
                                                Icons.Rounded.PowerSettingsNew
                                            },
                                            contentDescription = "تغییر وضعیت ضبط خودکار",
                                            tint = orbColor,
                                            modifier = Modifier.size(40.dp)
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
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when {
                                !permissionSummary.allEssentialGranted -> "برای شروع خودکار، دایره را لمس کنید"
                                settings.autoRecordEnabled -> "پایش و ضبط خودکار مکالمات فعال است"
                                else -> "ضبط خودکار تماس‌ها متوقف شده است"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "به محض پاسخ به تماس ورودی یا شروع تماس خروجی، صدا با وضوح بالا ضبط و ذخیره می‌شود.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InfoGlassPill(
                                label = "فرمت: ${settings.audioFormat.extension.uppercase()}",
                                icon = Icons.Rounded.CheckCircle,
                                tint = colors.cyan,
                                modifier = Modifier.weight(1f),
                                onClick = onOpenSettings
                            )
                            InfoGlassPill(
                                label = "${settings.sampleRateHz / 1000}kHz • ${settings.bitrateKbps}k",
                                icon = Icons.Rounded.Tune,
                                tint = colors.indigo,
                                modifier = Modifier.weight(1f),
                                onClick = onOpenSettings
                            )
                        }
                    }
                }
            }

            // Bento Row: Storage Gauge Tile + Direct Voice Recorder Tile
            item {
                val limitBytes = settings.storageLimit.limitBytes
                val usedRatio = if (limitBytes > 0L) {
                    (totalStorageBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f)
                } else {
                    0.18f
                }
                val gaugeColor = when {
                    limitBytes > 0L && usedRatio > 0.85f -> colors.crimson
                    limitBytes > 0L && usedRatio > 0.65f -> colors.amber
                    else -> colors.emerald
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        accentColor = colors.blue,
                        contentPadding = 14.dp,
                        onClick = onOpenSettings
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "فضای آرشیو",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = colors.blue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            StorageArcGauge(
                                usedRatio = usedRatio,
                                accentColor = gaugeColor,
                                modifier = Modifier.size(72.dp)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${recentRecordings.size}",
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "فایل",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${DeviceCompatibilityManager.formatFileSize(totalStorageBytes)} / ${settings.storageLimit.label}",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 11.sp,
                            color = colors.cyan,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        highlighted = liveState.isRecording,
                        accentColor = if (liveState.isRecording) colors.crimson else colors.cyan,
                        contentPadding = 14.dp
                    ) {
                        Text(
                            text = if (liveState.isRecording) "ضبط فعال" else "ضبط فوری صدا",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (liveState.isRecording) colors.crimson else colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (liveState.isRecording) {
                                DeviceCompatibilityManager.formatDuration(liveState.elapsedMs)
                            } else {
                                "ضبط مستقیم و آنی با فرمت ${settings.audioFormat.extension.uppercase()}"
                            },
                            fontFamily = if (liveState.isRecording) SpaceGroteskFontFamily else null,
                            fontWeight = if (liveState.isRecording) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (liveState.isRecording) 18.sp else 12.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LiveAmplitudeBars(
                            amplitude = liveState.amplitude,
                            isActive = liveState.isRecording,
                            barColor = colors.crimson,
                            modifier = Modifier.height(24.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (!permissionSummary.hasRecordAudio) {
                                    onRequestPermissions()
                                } else {
                                    onToggleManualRecord()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (liveState.isRecording) colors.crimson else colors.cyan,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag(if (liveState.isRecording) "stop_recording_button" else "manual_record_button")
                        ) {
                            Icon(
                                imageVector = if (liveState.isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (liveState.isRecording) "توقف و ذخیره" else "شروع ضبط",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (!liveState.errorMessage.isNullOrBlank()) {
                item {
                    GlassCard(highlighted = true, accentColor = colors.amber, contentPadding = 12.dp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = colors.amber)
                            Text(liveState.errorMessage, style = MaterialTheme.typography.bodyMedium, color = colors.amber)
                        }
                    }
                }
            }

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
                        color = colors.textPrimary
                    )
                    if (recentRecordings.isNotEmpty()) {
                        TextButton(
                            onClick = onOpenAllRecordings,
                            modifier = Modifier.testTag("see_all_recordings_button")
                        ) {
                            Text("مشاهده تاریخچه کامل", color = colors.cyan)
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
                                tint = colors.cyan.copy(alpha = 0.7f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "هنوز مکالمه‌ای ثبت نشده است",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "با برقراری اولین تماس یا لمس «شروع ضبط»، فایل صوتی با موج صدا اینجا نمایش داده می‌شود.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
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
                        onAssignContactClick = {
                            onLoadContacts("")
                            contactTarget = recording
                        },
                        onRenameClick = { renameTarget = recording },
                        onShareClick = { onShareRecording(recording) },
                        onExportClick = { onExportRecording(recording) },
                        onDeleteClick = { deleteTarget = recording }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(92.dp)) }
        }
    }

    contactTarget?.let { target ->
        ContactPickerDialog(
            contacts = contacts,
            hasPermission = permissionSummary.hasReadContacts,
            onSearch = onLoadContacts,
            onRequestPermission = onRequestPermissions,
            onSelectContact = { name ->
                onAssignContact(target.id, name)
                contactTarget = null
            },
            onDismiss = { contactTarget = null }
        )
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
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CyberiTheme.colors
    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(14.dp),
        color = colors.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.42f)),
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
                color = colors.textPrimary,
                maxLines = 1
            )
        }
    }
}
