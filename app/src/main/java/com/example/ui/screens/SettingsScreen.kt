package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioFormatOption
import com.example.model.AudioSourceOption
import com.example.model.RecordingSettings
import com.example.model.StorageLimitOption
import com.example.service.PermissionSummary
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberiTheme
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.util.DeviceCompatibilityManager
import com.example.util.DeviceCompatibilityStatus
import com.example.util.SupportActions
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    totalStorageBytes: Long,
    deviceStatus: DeviceCompatibilityStatus,
    permissionSummary: PermissionSummary,
    onUpdateSettings: ((RecordingSettings) -> RecordingSettings) -> Unit,
    onSelectFormat: (AudioFormatOption) -> Unit,
    onSelectSource: (AudioSourceOption) -> Unit,
    onSelectStorageLimit: (StorageLimitOption) -> Unit,
    onCleanupStorageNow: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenSystemAppSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenSupportEmail: () -> Unit,
    onOpenSupportDialer: () -> Unit,
    onShareApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CyberiTheme.colors

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "تنظیمات حرفه‌ای سایبری",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "مدیریت ضبط خودکار، فرمت‌های صوتی، سقف حافظه، دسترسی‌ها و پشتیبانی",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                }
            }

            // 1. Automatic Call Recording Rules
            item {
                GlassCard(accentColor = colors.cyan) {
                    SectionHeader(
                        title = "ضبط خودکار تماس‌ها",
                        icon = Icons.Rounded.SettingsSuggest,
                        tint = colors.cyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "فعال‌سازی ضبط خودکار",
                        subtitle = "شروع و پایان خودکار ضبط هنگام برقراری تماس",
                        checked = settings.autoRecordEnabled,
                        testTag = "switch_auto_record",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(autoRecordEnabled = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "ضبط تماس‌های ورودی",
                        subtitle = "به‌محض پاسخ به زنگ، ضبط صدا شروع شود",
                        checked = settings.recordIncomingCalls,
                        testTag = "switch_incoming",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(recordIncomingCalls = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "ضبط تماس‌های خروجی",
                        subtitle = "از لحظه اول شماره‌گیری و برقراری تماس ضبط شود",
                        checked = settings.recordOutgoingCalls,
                        testTag = "switch_outgoing",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(recordOutgoingCalls = enabled) }
                        }
                    )
                    GlassDivider()

                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تاخیر شروع ضبط تماس", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                            Text(
                                text = if (settings.startDelaySeconds == 0) "لحظه‌ای (۰ ثانیه)" else "${settings.startDelaySeconds} ثانیه",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.cyan
                            )
                        }
                        Slider(
                            value = settings.startDelaySeconds.toFloat(),
                            onValueChange = { valSec ->
                                onUpdateSettings { it.copy(startDelaySeconds = valSec.roundToInt()) }
                            },
                            valueRange = 0f..3f,
                            steps = 2,
                            colors = SliderDefaults.colors(thumbColor = colors.cyan, activeTrackColor = colors.cyan)
                        )
                    }

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حداقل مدت ذخیره مکالمه", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                            Text(
                                text = "${settings.minDurationSeconds} ثانیه",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.cyan
                            )
                        }
                        Text(
                            text = "تماس‌های کوتاه‌تر از این مقدار ذخیره نمی‌شوند",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                        Slider(
                            value = settings.minDurationSeconds.toFloat(),
                            onValueChange = { valSec ->
                                onUpdateSettings { it.copy(minDurationSeconds = valSec.roundToInt()) }
                            },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = colors.cyan, activeTrackColor = colors.cyan)
                        )
                    }
                }
            }

            // 2. Storage Limits & Quota Management
            item {
                val limitBytes = settings.storageLimit.limitBytes
                val usedRatio = if (limitBytes > 0L) {
                    (totalStorageBytes.toFloat() / limitBytes.toFloat()).coerceIn(0f, 1f)
                } else 0.08f
                val barColor = when {
                    limitBytes > 0L && usedRatio > 0.85f -> colors.crimson
                    limitBytes > 0L && usedRatio > 0.65f -> colors.amber
                    else -> colors.blue
                }

                GlassCard(highlighted = true, accentColor = colors.blue) {
                    SectionHeader(
                        title = "مدیریت سقف فضای ذخیره‌سازی",
                        icon = Icons.Rounded.Storage,
                        tint = colors.blue
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مصرف فعلی: ${DeviceCompatibilityManager.formatFileSize(totalStorageBytes)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "سقف: ${settings.storageLimit.label}",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = colors.cyan
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { usedRatio },
                        color = barColor,
                        trackColor = colors.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("انتخاب سقف مجاز آرشیو", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StorageLimitOption.entries.forEach { option ->
                            val selected = settings.storageLimit == option
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectStorageLimit(option) },
                                label = { Text(option.label, fontFamily = SpaceGroteskFontFamily) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.blue.copy(alpha = 0.20f),
                                    selectedLabelColor = colors.blue,
                                    containerColor = colors.surface.copy(alpha = 0.7f),
                                    labelColor = colors.textSecondary
                                ),
                                border = BorderStroke(1.dp, if (selected) colors.blue else colors.glassBorderSubtle),
                                modifier = Modifier.testTag("storage_limit_${option.id}")
                            )
                        }
                    }

                    GlassDivider()
                    SettingSwitchRow(
                        title = "پاکسازی هوشمند هنگام پر شدن سقف",
                        subtitle = "قدیمی‌ترین فایل‌های غیرنشان‌شده را به‌طور خودکار حذف می‌کند",
                        checked = settings.autoDeleteOldestOnLimit,
                        testTag = "switch_auto_delete_oldest",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(autoDeleteOldestOnLimit = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "ذخیره خودکار در پوشه Music/Cyberi",
                        subtitle = "کپی قابل مشاهده در مدیر فایل گوشی علاوه بر آرشیو داخلی",
                        checked = settings.autoExportToMusic,
                        testTag = "switch_auto_export",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(autoExportToMusic = enabled) }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onCleanupStorageNow,
                        border = BorderStroke(1.dp, colors.cyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("cleanup_storage_button")
                    ) {
                        Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اجرای پاکسازی بر اساس سقف انتخابی", color = colors.cyan)
                    }
                }
            }

            // 3. All Audio Formats Selector
            item {
                GlassCard(accentColor = colors.emerald) {
                    SectionHeader(
                        title = "فرمت صوتی ضبط (پشتیبانی کامل)",
                        icon = Icons.Rounded.AudioFile,
                        tint = colors.emerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "فرمت M4A (AAC-LC) به‌عنوان فرمت اصلی و استاندارد انتخاب شده است:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AudioFormatOption.entries.forEach { format ->
                        val isSelected = settings.audioFormat == format
                        val supported = format.isSupportedOnDevice()
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .testTag("format_option_${format.id}"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colors.cyan.copy(alpha = 0.12f) else colors.surface.copy(alpha = 0.7f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) colors.cyan else colors.glassBorderSubtle
                            ),
                            onClick = { if (supported) onSelectFormat(format) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) colors.cyan else colors.textMuted
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = format.displayName,
                                            fontFamily = SpaceGroteskFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (supported) colors.textPrimary else colors.textMuted
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (format.isPrimaryDefault) {
                                                colors.emerald.copy(alpha = 0.16f)
                                            } else {
                                                colors.indigo.copy(alpha = 0.16f)
                                            }
                                        ) {
                                            Text(
                                                text = if (supported) format.shortBadge else "نیاز به اندروید ۱۰+",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (format.isPrimaryDefault) colors.emerald else colors.indigo,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = format.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(5) { idx ->
                                        Surface(
                                            shape = CircleShape,
                                            color = if (idx < format.qualityLevel) colors.cyan else colors.textMuted.copy(alpha = 0.25f),
                                            modifier = Modifier.size(6.dp)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Audio Clarity: Sample Rate, Bitrate, Channels
            item {
                GlassCard(accentColor = colors.indigo) {
                    SectionHeader(
                        title = "تنظیمات وضوح و کیفیت صدا",
                        icon = Icons.Rounded.GraphicEq,
                        tint = colors.indigo
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("نرخ نمونه‌برداری (Sample Rate)", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(16000 to "16 kHz", 32000 to "32 kHz", 44100 to "44.1 kHz", 48000 to "48 kHz (HD)").forEach { (rate, label) ->
                            val selected = settings.sampleRateHz == rate
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateSettings { it.copy(sampleRateHz = rate) } },
                                label = { Text(label, fontFamily = SpaceGroteskFontFamily) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.indigo.copy(alpha = 0.20f),
                                    selectedLabelColor = colors.indigo
                                ),
                                modifier = Modifier.testTag("sample_rate_$rate")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("نرخ بیت (Bitrate)", style = MaterialTheme.typography.labelLarge, color = colors.textPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(64, 128, 192, 256, 320).forEach { kbps ->
                            val selected = settings.bitrateKbps == kbps
                            FilterChip(
                                selected = selected,
                                onClick = { onUpdateSettings { it.copy(bitrateKbps = kbps) } },
                                label = { Text("$kbps kbps", fontFamily = SpaceGroteskFontFamily) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.indigo.copy(alpha = 0.20f),
                                    selectedLabelColor = colors.indigo
                                ),
                                modifier = Modifier.testTag("bitrate_$kbps")
                            )
                        }
                    }

                    GlassDivider()
                    SettingSwitchRow(
                        title = "ضبط دوکاناله (Stereo)",
                        subtitle = "مناسب گوشی‌های دو میکروفونی",
                        checked = settings.stereoChannels,
                        testTag = "switch_stereo",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(stereoChannels = enabled) }
                        }
                    )
                }
            }

            // 5. Remote Caller Voice Clarity & Audio Source
            item {
                GlassCard(accentColor = colors.amber) {
                    SectionHeader(
                        title = "صدای طرف مقابل و پردازش هوشمند",
                        icon = Icons.Rounded.RecordVoiceOver,
                        tint = colors.amber
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    AudioSourceOption.entries.forEach { source ->
                        val isSelected = settings.audioSource == source
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSource(source) }
                                .padding(vertical = 8.dp)
                                .testTag("source_option_${source.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSelected) colors.cyan else colors.textMuted
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(source.title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                                Text(source.description, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                            }
                        }
                    }

                    GlassDivider()
                    SettingSwitchRow(
                        title = "حالت پل تماس دوطرفه (تقویت خط تماس)",
                        subtitle = "دریافت قدرتمند صدای شخصی که پشت خط است و بهینه‌سازی ضبط دوطرفه",
                        checked = settings.twoWayBridgeBoost,
                        testTag = "switch_two_way_bridge",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(twoWayBridgeBoost = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "تقویت صدای طرف مقابل با بلندگوی خودکار",
                        subtitle = "در تماس‌ها بلندگو را فعال می‌کند تا صدای طرف مقابل با حداکثر قدرت ضبط شود",
                        checked = settings.autoSpeakerBoost,
                        testTag = "switch_auto_speaker",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(autoSpeakerBoost = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "حذف نویز محیط (Noise Suppressor)",
                        subtitle = if (deviceStatus.supportsNoiseSuppressor) "توصیه برای ضبط دوطرفه: خاموش تا صدای طرف مقابل حذف نشود" else "پشتیبانی استاندارد",
                        checked = settings.noiseSuppression,
                        testTag = "switch_noise_suppressor",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(noiseSuppression = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "تقویت خودکار صدا (AGC)",
                        subtitle = "افزایش هوشمند صدای ضعیف طرف مقابل هنگام مکالمه",
                        checked = settings.autoGainControl,
                        testTag = "switch_agc",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(autoGainControl = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "حذف اکو (Echo Canceler)",
                        subtitle = "توصیه برای ضبط دوطرفه: خاموش تا صدای گوشی فیلتر نشود",
                        checked = settings.echoCancellation,
                        testTag = "switch_aec",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(echoCancellation = enabled) }
                        }
                    )

                    GlassDivider()
                    SettingSwitchRow(
                        title = "دکمه شناور ضبط فوری روی صفحه تماس",
                        subtitle = "نمایش کلید کوچک قابل جابجایی هنگام مکالمه برای ضبط با یک لمس",
                        checked = settings.floatingButtonOnCalls,
                        testTag = "switch_floating_on_calls",
                        onCheckedChange = { enabled ->
                            if (enabled && !permissionSummary.hasOverlayPermission) {
                                onOpenOverlaySettings()
                            }
                            onUpdateSettings { it.copy(floatingButtonOnCalls = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "نمایش همیشگی دکمه شناور (جابجایی و دسترسی سریع)",
                        subtitle = "دکمه شناور روی صفحه نمایش بماند تا جای دلخواه آن را تنظیم کنید",
                        checked = settings.floatingButtonAlwaysOn,
                        testTag = "switch_floating_always",
                        onCheckedChange = { enabled ->
                            if (enabled && !permissionSummary.hasOverlayPermission) {
                                onOpenOverlaySettings()
                            }
                            onUpdateSettings { it.copy(floatingButtonAlwaysOn = enabled) }
                        }
                    )

                    if (!permissionSummary.hasOverlayPermission) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = onOpenOverlaySettings,
                            border = BorderStroke(1.dp, colors.indigo.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("settings_overlay_button")
                        ) {
                            Text("فعال‌سازی اجازه نمایش کلید شناور روی تماس‌ها", color = colors.indigo)
                        }
                    }

                    GlassDivider()
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ضریب تقویت نرم‌افزاری صدا", style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
                            Text(
                                text = "${settings.micGainPercent}%",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.cyan
                            )
                        }
                        Slider(
                            value = settings.micGainPercent.toFloat(),
                            onValueChange = { valPct ->
                                onUpdateSettings { it.copy(micGainPercent = valPct.roundToInt()) }
                            },
                            valueRange = 100f..250f,
                            steps = 14,
                            colors = SliderDefaults.colors(thumbColor = colors.cyan, activeTrackColor = colors.cyan)
                        )
                    }
                }
            }

            // 6. Permission Handling Service & Device Info
            item {
                GlassCard(accentColor = colors.emerald) {
                    SectionHeader(
                        title = "سرویس دسترسی‌ها و همگام‌سازی گوشی",
                        icon = Icons.Rounded.Security,
                        tint = colors.emerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "نمایش اعلان پس از ذخیره مکالمه",
                        subtitle = "اطلاع‌رسانی کوتاه پس از پایان هر تماس",
                        checked = settings.showNotifications,
                        testTag = "switch_notifications",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(showNotifications = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "سرویس پایش دائمی پس‌زمینه",
                        subtitle = "آماده نگه داشتن موتور تشخیص تماس برای شروع بدون تاخیر",
                        checked = settings.keepMonitorServiceAlive,
                        testTag = "switch_keep_alive",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(keepMonitorServiceAlive = enabled) }
                        }
                    )

                    GlassDivider()
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${deviceStatus.deviceModel} • ${deviceStatus.androidVersionLabel}",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    permissionSummary.items.forEach { perm ->
                        PermissionStatusLine(perm.title, perm.isGranted)
                    }
                    PermissionStatusLine("نمایش کلید شناور روی صفحه تماس", permissionSummary.hasOverlayPermission)
                    PermissionStatusLine("معافیت بهینه‌سازی باتری", permissionSummary.isBatteryOptimizationIgnored)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.cyan, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_grant_permissions")
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("درخواست مجوزها", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenBatterySettings,
                            border = BorderStroke(1.dp, colors.emerald.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_battery_button")
                        ) {
                            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = colors.emerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("پس‌زمینه و باتری", color = colors.emerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenSystemAppSettings,
                        border = BorderStroke(1.dp, colors.glassBorderSubtle),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("باز کردن صفحه تنظیمات سیستم سایبری", color = colors.textSecondary)
                    }
                }
            }

            // 7. درباره ما (About Us + Gmail + Direct Dialer + Share App)
            item {
                GlassCard(
                    highlighted = true,
                    accentColor = colors.indigo
                ) {
                    SectionHeader(
                        title = "درباره ما",
                        icon = Icons.Rounded.Info,
                        tint = colors.indigo
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Line 1: Designer & Creator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.indigo.copy(alpha = 0.14f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.indigo, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = "طراحی و ایجاد شده توسط:  ${SupportActions.DEVELOPER_NAME}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Line 2: Support Channels Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.cyan.copy(alpha = 0.14f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.SupportAgent, contentDescription = null, tint = colors.cyan, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(
                            text = "راه‌های ارتباطی برای دریافت پشتیبانی و راهنمایی:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Direct Gmail Support Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("about_email_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = colors.crimson.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, colors.crimson.copy(alpha = 0.45f)),
                        onClick = onOpenSupportEmail
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Email,
                                    contentDescription = "ایمیل پشتیبانی",
                                    tint = colors.crimson,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = SupportActions.SUPPORT_EMAIL,
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                            }
                            Text(
                                text = "ارسال جیمیل",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.crimson
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Direct Phone Dialer Button
                    Button(
                        onClick = onOpenSupportDialer,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.emerald,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("about_phone_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Call,
                            contentDescription = "تماس تلفنی",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "شماره تماس: ${SupportActions.SUPPORT_PHONE}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Share App Button
                    Button(
                        onClick = onShareApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.indigo,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("about_share_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "اشتراک‌گذاری برنامه",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اشتراک‌گذاری برنامه سایبری با دیگران",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(92.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    tint: Color
) {
    val colors = CyberiTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = CyberiTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.cyan,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.surface
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun GlassDivider() {
    val colors = CyberiTheme.colors
    HorizontalDivider(
        color = colors.glassBorderSubtle,
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun PermissionStatusLine(label: String, granted: Boolean) {
    val colors = CyberiTheme.colors
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (granted) colors.emerald else colors.amber,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (granted) colors.textPrimary else colors.amber
        )
    }
}
