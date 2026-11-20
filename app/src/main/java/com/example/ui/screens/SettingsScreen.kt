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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioFormatOption
import com.example.model.AudioSourceOption
import com.example.model.RecordingSettings
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DeviceCompatibilityStatus
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: RecordingSettings,
    deviceStatus: DeviceCompatibilityStatus,
    onUpdateSettings: ((RecordingSettings) -> RecordingSettings) -> Unit,
    onSelectFormat: (AudioFormatOption) -> Unit,
    onSelectSource: (AudioSourceOption) -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenSystemAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "تنظیمات حرفه‌ای سایبری",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "پیکربندی کامل فرمت‌های صوتی، وضوح ضبط و عملکرد هوشمند",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // 1. Automatic Call Recording Rules
            item {
                GlassCard {
                    SectionHeader(
                        title = "ضبط خودکار تماس‌ها",
                        icon = Icons.Rounded.SettingsSuggest,
                        tint = CyberCyan
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

                    // Start Delay Slider
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تاخیر شروع ضبط تماس", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                text = if (settings.startDelaySeconds == 0) "لحظه‌ای (۰ ثانیه)" else "${settings.startDelaySeconds} ثانیه",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan
                            )
                        }
                        Slider(
                            value = settings.startDelaySeconds.toFloat(),
                            onValueChange = { valSec ->
                                onUpdateSettings { it.copy(startDelaySeconds = valSec.roundToInt()) }
                            },
                            valueRange = 0f..3f,
                            steps = 2,
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )
                    }

                    // Min Duration Filter Slider
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حداقل مدت ذخیره مکالمه", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                text = "${settings.minDurationSeconds} ثانیه",
                                style = MaterialTheme.typography.labelLarge,
                                color = CyberCyan
                            )
                        }
                        Text(
                            text = "تماس‌های کوتاه‌تر از این مقدار برای جلوگیری از شلوغی آرشیو ذخیره نمی‌شوند",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Slider(
                            value = settings.minDurationSeconds.toFloat(),
                            onValueChange = { valSec ->
                                onUpdateSettings { it.copy(minDurationSeconds = valSec.roundToInt()) }
                            },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )
                    }
                }
            }

            // 2. All Audio Formats Selector
            item {
                GlassCard {
                    SectionHeader(
                        title = "فرمت صوتی ضبط (پشتیبانی از تمام فرمت‌ها)",
                        icon = Icons.Rounded.AudioFile,
                        tint = CyberEmerald
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "فرمت M4A (AAC-LC) به‌عنوان فرمت اصلی برای تعادل عالی بین وضوح بالا و سازگاری کامل انتخاب شده است:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
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
                            color = if (isSelected) CyberCyan.copy(alpha = 0.14f) else CyberSurface.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) CyberCyan else GlassBorderSubtle
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
                                    tint = if (isSelected) CyberCyan else TextMuted
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
                                            color = if (supported) TextPrimary else TextMuted
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (format.isPrimaryDefault) {
                                                CyberEmerald.copy(alpha = 0.18f)
                                            } else {
                                                CyberCyan.copy(alpha = 0.14f)
                                            }
                                        ) {
                                            Text(
                                                text = if (supported) format.shortBadge else "نیاز به اندروید ۱۰+",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (format.isPrimaryDefault) CyberEmerald else CyberCyan,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = format.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Audio Clarity: Sample Rate, Bitrate, Channels
            item {
                GlassCard {
                    SectionHeader(
                        title = "تنظیمات وضوح و کیفیت صدا",
                        icon = Icons.Rounded.GraphicEq,
                        tint = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("نرخ نمونه‌برداری (Sample Rate)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
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
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = CyberCyan
                                ),
                                modifier = Modifier.testTag("sample_rate_$rate")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("نرخ بیت (Bitrate)", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
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
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = CyberCyan
                                ),
                                modifier = Modifier.testTag("bitrate_$kbps")
                            )
                        }
                    }

                    GlassDivider()
                    SettingSwitchRow(
                        title = "ضبط دوکاناله (Stereo)",
                        subtitle = "مناسب گوشی‌های دارای دو میکروفون؛ حالت مونو برای مکالمه متمرکزتر است",
                        checked = settings.stereoChannels,
                        testTag = "switch_stereo",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(stereoChannels = enabled) }
                        }
                    )
                }
            }

            // 4. Remote Caller Voice Clarity & Audio Source
            item {
                GlassCard {
                    SectionHeader(
                        title = "صدای طرف مقابل و پردازش هوشمند",
                        icon = Icons.Rounded.RecordVoiceOver,
                        tint = CyberAmber
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
                                tint = if (isSelected) CyberCyan else TextMuted
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(source.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                Text(source.description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }

                    GlassDivider()
                    SettingSwitchRow(
                        title = "تقویت صدای طرف مقابل با بلندگوی خودکار",
                        subtitle = "در تماس‌ها بلندگو را فعال می‌کند تا صدای طرف مقابل بسیار رسا و واضح ضبط شود",
                        checked = settings.autoSpeakerBoost,
                        testTag = "switch_auto_speaker",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(autoSpeakerBoost = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "حذف نویز محیط (Noise Suppressor)",
                        subtitle = if (deviceStatus.supportsNoiseSuppressor) "پشتیبانی سخت‌افزاری فعال روی گوشی شما" else "پشتیبانی استاندارد",
                        checked = settings.noiseSuppression,
                        testTag = "switch_noise_suppressor",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(noiseSuppression = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "تقویت خودکار صدا (AGC)",
                        subtitle = "بالا بردن خودکار صدای ضعیف طرف مقابل در ضبط WAV",
                        checked = settings.autoGainControl,
                        testTag = "switch_agc",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(autoGainControl = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "حذف اکو (Echo Canceler)",
                        subtitle = "جلوگیری از برگشت صدا هنگام استفاده از بلندگو",
                        checked = settings.echoCancellation,
                        testTag = "switch_aec",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(echoCancellation = enabled) }
                        }
                    )

                    GlassDivider()
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ضریب تقویت نرم‌افزاری صدا (حالت WAV)", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text(
                                text = "${settings.micGainPercent}%",
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan
                            )
                        }
                        Slider(
                            value = settings.micGainPercent.toFloat(),
                            onValueChange = { valPct ->
                                onUpdateSettings { it.copy(micGainPercent = valPct.roundToInt()) }
                            },
                            valueRange = 100f..240f,
                            steps = 13,
                            colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                        )
                    }
                }
            }

            // 5. Storage & Device Compatibility
            item {
                GlassCard {
                    SectionHeader(
                        title = "ذخیره‌سازی و سازگاری با گوشی",
                        icon = Icons.Rounded.Storage,
                        tint = CyberEmerald
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "ذخیره خودکار در پوشه Music/Cyberi",
                        subtitle = "علاوه بر داخل برنامه، فایل‌ها در مدیر فایل گوشی نیز قابل دسترس باشند",
                        checked = settings.autoExportToMusic,
                        testTag = "switch_auto_export",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(autoExportToMusic = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "نمایش اعلان پس از ذخیره مکالمه",
                        subtitle = "اطلاع‌رسانی کوتاه پس از ذخیره موفقیت‌آمیز تماس",
                        checked = settings.showNotifications,
                        testTag = "switch_notifications",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(showNotifications = enabled) }
                        }
                    )
                    GlassDivider()
                    SettingSwitchRow(
                        title = "سرویس پایش دائمی پس‌زمینه",
                        subtitle = "فعال نگه داشتن موتور تشخیص تماس برای شروع ضبط بدون تاخیر",
                        checked = settings.keepMonitorServiceAlive,
                        testTag = "switch_keep_alive",
                        onCheckedChange = { enabled ->
                            onUpdateSettings { it.copy(keepMonitorServiceAlive = enabled) }
                        }
                    )

                    GlassDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Hardware compatibility info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PhoneAndroid, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
                        Text(
                            text = "${deviceStatus.deviceModel} • ${deviceStatus.androidVersionLabel}",
                            fontFamily = SpaceGroteskFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    PermissionStatusLine("دسترسی میکروفون (RECORD_AUDIO)", deviceStatus.hasRecordAudio)
                    PermissionStatusLine("تشخیص وضعیت تماس (READ_PHONE_STATE)", deviceStatus.hasReadPhoneState)
                    PermissionStatusLine("دسترسی اعلان‌ها (POST_NOTIFICATIONS)", deviceStatus.hasPostNotifications)
                    PermissionStatusLine("معافیت بهینه‌سازی باتری", deviceStatus.isBatteryOptimizationIgnored)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onRequestPermissions,
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = CyberVoid),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_grant_permissions")
                        ) {
                            Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بررسی مجوزها", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenBatterySettings,
                            border = BorderStroke(1.dp, CyberEmerald.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("settings_battery_button")
                        ) {
                            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = CyberEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("باتری گوشی", color = CyberEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenSystemAppSettings,
                        border = BorderStroke(1.dp, GlassBorderSubtle),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("باز کردن صفحه تنظیمات سیستم سایبری", color = TextSecondary)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CyberVoid,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = CyberSurface
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun GlassDivider() {
    HorizontalDivider(
        color = GlassBorderSubtle,
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun PermissionStatusLine(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = if (granted) CyberEmerald else CyberAmber,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (granted) TextPrimary else CyberAmber
        )
    }
}
