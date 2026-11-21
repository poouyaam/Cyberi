package com.example.model

import android.media.MediaRecorder
import android.os.Build

enum class AudioFormatOption(
    val id: String,
    val displayName: String,
    val shortBadge: String,
    val extension: String,
    val mimeType: String,
    val description: String,
    val qualityLevel: Int,
    val isPrimaryDefault: Boolean = false,
    val minApiLevel: Int = 24
) {
    M4A_AAC(
        id = "M4A_AAC",
        displayName = "M4A (AAC-LC)",
        shortBadge = "اصلی و پیشنهادی",
        extension = "m4a",
        mimeType = "audio/mp4",
        description = "فرمت اصلی و حرفه‌ای؛ وضوح مکالمه عالی، حجم متعادل و قابل پخش در تمام گوشی‌ها",
        qualityLevel = 5,
        isPrimaryDefault = true
    ),
    WAV_PCM(
        id = "WAV_PCM",
        displayName = "WAV (PCM ۱۶ بیتی)",
        shortBadge = "بدون افت کیفیت",
        extension = "wav",
        mimeType = "audio/wav",
        description = "ضبط استودیویی بدون فشرده‌سازی با موتور تقویت هوشمند صدای طرف مقابل و حذف نویز",
        qualityLevel = 5
    ),
    OGG_OPUS(
        id = "OGG_OPUS",
        displayName = "OGG (Opus)",
        shortBadge = "کدک پیشرفته",
        extension = "ogg",
        mimeType = "audio/ogg",
        description = "وضوح فوق‌العاده صدای انسان با کدک Opus و حجم بسیار بهینه (اندروید ۱۰ به بالا)",
        qualityLevel = 4,
        minApiLevel = Build.VERSION_CODES.Q
    ),
    THREE_GP_AMR_WB(
        id = "THREE_GP_AMR_WB",
        displayName = "3GP (پهن‌باند)",
        shortBadge = "مخابراتی",
        extension = "3gp",
        mimeType = "audio/3gpp",
        description = "فرمت استاندارد ضبط تماس مخابراتی با حجم کم و سازگاری بالا",
        qualityLevel = 3
    ),
    WEBM_OPUS(
        id = "WEBM_OPUS",
        displayName = "WEBM (Opus/Vorbis)",
        shortBadge = "اشتراک‌گذاری سریع",
        extension = "webm",
        mimeType = "audio/webm",
        description = "مناسب برای اشتراک‌گذاری سریع با کیفیت صدای دیجیتال",
        qualityLevel = 4
    ),
    AMR_WB(
        id = "AMR_WB",
        displayName = "AMR-WB",
        shortBadge = "فوق‌فشرده HD",
        extension = "awb",
        mimeType = "audio/amr-wb",
        description = "فرمت اختصاصی صدای مکالمه پهن‌باند با کمترین اشغال حافظه",
        qualityLevel = 3
    ),
    AMR_NB(
        id = "AMR_NB",
        displayName = "AMR-NB",
        shortBadge = "حداقل حجم",
        extension = "amr",
        mimeType = "audio/amr",
        description = "ساده‌ترین فرمت فشرده با کوچک‌ترین حجم فایل برای آرشیو طولانی",
        qualityLevel = 2
    );

    fun isSupportedOnDevice(): Boolean = Build.VERSION.SDK_INT >= minApiLevel

    companion object {
        fun fromId(id: String?): AudioFormatOption =
            entries.firstOrNull { it.id == id } ?: M4A_AAC
    }
}

enum class AudioSourceOption(
    val id: String,
    val title: String,
    val description: String,
    val mediaRecorderSource: Int
) {
    AUTO_SMART(
        id = "AUTO_SMART",
        title = "هوشمند خودکار (پیشنهادی)",
        description = "به‌صورت خودکار بهترین ورودی سخت‌افزاری گوشی را برای ضبط صدای شما و طرف مقابل انتخاب می‌کند",
        mediaRecorderSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
    ),
    VOICE_COMMUNICATION(
        id = "VOICE_COMMUNICATION",
        title = "ارتباط صوتی دوطرفه (VoIP/Call)",
        description = "مسیر بهینه‌شده برای تماس‌ها با حذف اکو و تفکیک صدای مکالمه",
        mediaRecorderSource = MediaRecorder.AudioSource.VOICE_COMMUNICATION
    ),
    VOICE_CALL(
        id = "VOICE_CALL",
        title = "خط مستقیم تماس (Voice Call)",
        description = "تلاش برای ضبط مستقیم صدای خط ورودی و خروجی (با جایگزینی خودکار در صورت محدودیت سیستم)",
        mediaRecorderSource = MediaRecorder.AudioSource.VOICE_CALL
    ),
    VOICE_RECOGNITION(
        id = "VOICE_RECOGNITION",
        title = "حساسیت بالا (مناسب بلندگو)",
        description = "تقویت دریافت صدا از محیط و بلندگو برای واضح‌تر شدن صدای طرف مقابل",
        mediaRecorderSource = MediaRecorder.AudioSource.VOICE_RECOGNITION
    ),
    MIC(
        id = "MIC",
        title = "میکروفون استاندارد",
        description = "ضبط مستقیم از میکروفون اصلی دستگاه بدون فیلتر مخابراتی",
        mediaRecorderSource = MediaRecorder.AudioSource.MIC
    );

    companion object {
        fun fromId(id: String?): AudioSourceOption =
            entries.firstOrNull { it.id == id } ?: VOICE_CALL
    }
}

enum class StorageLimitOption(
    val id: String,
    val label: String,
    val limitBytes: Long
) {
    MB_250("MB_250", "250 MB", 250L * 1024L * 1024L),
    MB_500("MB_500", "500 MB", 500L * 1024L * 1024L),
    GB_1("GB_1", "1 GB", 1024L * 1024L * 1024L),
    GB_2("GB_2", "2 GB", 2L * 1024L * 1024L * 1024L),
    GB_5("GB_5", "5 GB", 5L * 1024L * 1024L * 1024L),
    UNLIMITED("UNLIMITED", "نامحدود", -1L);

    companion object {
        fun fromId(id: String?): StorageLimitOption =
            entries.firstOrNull { it.id == id } ?: GB_1
    }
}

enum class CallType(val id: String, val label: String) {
    INCOMING("INCOMING", "تماس ورودی"),
    OUTGOING("OUTGOING", "تماس خروجی"),
    MANUAL("MANUAL", "ضبط دستی");

    companion object {
        fun fromId(id: String?): CallType =
            entries.firstOrNull { it.id == id } ?: MANUAL
    }
}

data class RecordingSettings(
    val autoRecordEnabled: Boolean = true,
    val recordIncomingCalls: Boolean = true,
    val recordOutgoingCalls: Boolean = true,
    val audioFormat: AudioFormatOption = AudioFormatOption.M4A_AAC,
    val audioSource: AudioSourceOption = AudioSourceOption.VOICE_CALL,
    val sampleRateHz: Int = 44100,
    val bitrateKbps: Int = 128,
    val stereoChannels: Boolean = false,
    val noiseSuppression: Boolean = false,
    val autoGainControl: Boolean = true,
    val echoCancellation: Boolean = false,
    val micGainPercent: Int = 180,
    val autoSpeakerBoost: Boolean = false,
    val twoWayBridgeBoost: Boolean = true,
    val floatingButtonOnCalls: Boolean = true,
    val floatingButtonAlwaysOn: Boolean = false,
    val startDelaySeconds: Int = 0,
    val minDurationSeconds: Int = 2,
    val autoExportToMusic: Boolean = true,
    val showNotifications: Boolean = true,
    val keepMonitorServiceAlive: Boolean = true,
    val storageLimit: StorageLimitOption = StorageLimitOption.GB_1,
    val autoDeleteOldestOnLimit: Boolean = true
)
