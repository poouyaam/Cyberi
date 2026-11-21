package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.CallAudioCaptureEngine
import com.example.audio.MediaStoreExporter
import com.example.data.db.CyberiDatabase
import com.example.data.db.RecordingEntity
import com.example.data.repository.RecordingRepository
import com.example.data.repository.SettingsRepository
import com.example.model.AudioFormatOption
import com.example.model.CallType
import com.example.util.DeviceCompatibilityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LiveRecorderState(
    val isMonitoring: Boolean = false,
    val isRecording: Boolean = false,
    val currentCallType: CallType? = null,
    val elapsedMs: Long = 0L,
    val amplitude: Int = 0,
    val activeFormat: AudioFormatOption = AudioFormatOption.M4A_AAC,
    val activeSourceLabel: String = "",
    val lastSavedTitle: String? = null,
    val errorMessage: String? = null
)

class CallRecorderService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var recordingRepository: RecordingRepository
    private lateinit var notificationManager: NotificationManager
    private var floatingButtonController: FloatingCallButtonController? = null

    private var captureEngine: CallAudioCaptureEngine? = null
    private var recordingStartTimeMs: Long = 0L
    private var activeCallType: CallType = CallType.MANUAL
    private var activeSampleRate: Int = 44100
    private var activeBitrate: Int = 128
    private var timerJob: Job? = null
    private var delayedStartJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var previousAudioMode: Int? = null
    private var previousSpeakerState: Boolean? = null
    private var previousCallVolume: Int? = null
    private var isInTelephonyCall: Boolean = false

    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: Any? = null
    @Suppress("DEPRECATION")
    private var legacyPhoneListener: PhoneStateListener? = null
    private var telephonyRegistered = false
    private var isForegroundActive = false

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        val dao = CyberiDatabase.getInstance(applicationContext).recordingDao()
        recordingRepository = RecordingRepository(dao, applicationContext)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        floatingButtonController = FloatingCallButtonController(applicationContext) {
            toggleInstantFromOverlay()
        }
        createNotificationChannels()
        registerTelephonyListenerIfPermitted()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        registerTelephonyListenerIfPermitted()

        when (intent?.action) {
            ACTION_START_MONITORING -> {
                if (ensureForeground(isRecording = false)) {
                    _liveState.update { it.copy(isMonitoring = true, errorMessage = null) }
                    syncFloatingOverlayVisibility()
                } else {
                    stopSelf()
                }
            }

            ACTION_STOP_MONITORING -> {
                if (!_liveState.value.isRecording) {
                    _liveState.update { it.copy(isMonitoring = false) }
                    floatingButtonController?.hide()
                    stopForegroundAndSelf()
                }
            }

            ACTION_START_CALL_RECORDING -> {
                val typeId = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CallType.MANUAL.id
                val callType = CallType.fromId(typeId)
                if (callType != CallType.MANUAL) {
                    isInTelephonyCall = true
                }
                if (ensureForeground(isRecording = true, callType = callType)) {
                    beginRecording(callType)
                } else {
                    _liveState.update {
                        it.copy(errorMessage = "سیستم اجازه اجرای سرویس ضبط در پس‌زمینه را نداد؛ دسترسی باتری و شروع خودکار را بررسی کنید")
                    }
                    stopSelf()
                }
            }

            ACTION_STOP_RECORDING -> {
                isInTelephonyCall = false
                finishRecording()
            }

            ACTION_SHOW_CALL_OVERLAY -> {
                isInTelephonyCall = true
                if (!isForegroundActive) {
                    ensureForeground(isRecording = _liveState.value.isRecording)
                }
                syncFloatingOverlayVisibility()
            }

            ACTION_HIDE_CALL_OVERLAY -> {
                isInTelephonyCall = false
                syncFloatingOverlayVisibility()
            }

            ACTION_SYNC_OVERLAY -> {
                syncFloatingOverlayVisibility()
            }

            ACTION_TOGGLE_INSTANT_RECORD -> {
                toggleInstantFromOverlay()
            }

            else -> {
                serviceScope.launch {
                    val settings = settingsRepository.getCurrentSettings()
                    if (settings.autoRecordEnabled && settings.keepMonitorServiceAlive && hasMicPermission()) {
                        if (ensureForeground(isRecording = false)) {
                            _liveState.update { it.copy(isMonitoring = true) }
                            syncFloatingOverlayVisibility()
                        } else {
                            stopSelf()
                        }
                    } else {
                        stopSelf()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun toggleInstantFromOverlay() {
        if (_liveState.value.isRecording) {
            finishRecording()
        } else {
            val callType = if (isInTelephonyCall) CallType.INCOMING else CallType.MANUAL
            if (ensureForeground(isRecording = true, callType = callType)) {
                beginRecording(callType)
            }
        }
    }

    private fun syncFloatingOverlayVisibility() {
        serviceScope.launch {
            val settings = settingsRepository.getCurrentSettings()
            val shouldShow = settings.floatingButtonAlwaysOn ||
                (settings.floatingButtonOnCalls && (isInTelephonyCall || _liveState.value.isRecording))
            if (shouldShow) {
                floatingButtonController?.show(
                    isRecording = _liveState.value.isRecording,
                    elapsedMs = _liveState.value.elapsedMs
                )
            } else {
                floatingButtonController?.hide()
            }
        }
    }

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureForeground(isRecording: Boolean, callType: CallType? = null): Boolean {
        val notification = if (isRecording) {
            buildRecordingNotification(callType ?: activeCallType, 0L)
        } else {
            buildMonitoringNotification()
        }
        val started = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasMicPermission()) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
                if (fgType != 0) {
                    startForeground(NOTIFICATION_ID_SERVICE, notification, fgType)
                } else {
                    startForeground(NOTIFICATION_ID_SERVICE, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID_SERVICE, notification)
            }
            true
        }.getOrDefault(false)
        isForegroundActive = started
        return started
    }

    /**
     * Verifies a writable recording directory (prefers external app Recordings dir,
     * falling back safely to internal filesDir/recordings if unavailable).
     */
    private fun resolveWritableOutputDir(): File? {
        val dirType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Environment.DIRECTORY_RECORDINGS
        } else {
            "Recordings"
        }
        val externalDir = getExternalFilesDir(dirType)
        if (externalDir != null && (externalDir.exists() || externalDir.mkdirs()) && externalDir.canWrite()) {
            return externalDir
        }
        val internalDir = File(filesDir, "recordings")
        if ((internalDir.exists() || internalDir.mkdirs()) && internalDir.canWrite()) {
            return internalDir
        }
        return null
    }

    private fun beginRecording(callType: CallType) {
        if (_liveState.value.isRecording) return
        if (!hasMicPermission()) {
            _liveState.update { it.copy(errorMessage = "دسترسی میکروفون صادر نشده است") }
            return
        }

        delayedStartJob?.cancel()
        delayedStartJob = serviceScope.launch {
            val settings = settingsRepository.getCurrentSettings()

            if (callType != CallType.MANUAL && settings.startDelaySeconds > 0) {
                delay(settings.startDelaySeconds * 1000L)
            }

            val outputDir = resolveWritableOutputDir()
            if (outputDir == null) {
                _liveState.update {
                    it.copy(
                        isRecording = false,
                        errorMessage = "مسیر ذخیره‌سازی قابل نوشتن در حافظه گوشی یافت نشد"
                    )
                }
                return@launch
            }

            acquireWakeLock()
            applyCallAudioBoostIfNeeded(callType, settings.autoSpeakerBoost, settings.twoWayBridgeBoost)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val prefix = when (callType) {
                CallType.INCOMING -> "IN"
                CallType.OUTGOING -> "OUT"
                CallType.MANUAL -> "REC"
            }
            val fileName = "Cyberi_${prefix}_$timeStamp.${settings.audioFormat.extension}"
            val outFile = File(outputDir, fileName)

            val engine = CallAudioCaptureEngine(
                context = applicationContext,
                outputFile = outFile,
                settings = settings,
                callType = callType,
                onAmplitude = { amp ->
                    _liveState.update { it.copy(amplitude = amp) }
                },
                onSourceChanged = { fmt, srcLabel ->
                    _liveState.update {
                        it.copy(activeFormat = fmt, activeSourceLabel = srcLabel)
                    }
                }
            )

            val started = engine.start(serviceScope)
            if (!started) {
                restoreAudioState()
                releaseWakeLock()
                _liveState.update {
                    it.copy(
                        isRecording = false,
                        errorMessage = "شروع ضبط صدا روی این مسیر صوتی با خطا مواجه شد"
                    )
                }
                if (!settings.keepMonitorServiceAlive) {
                    stopForegroundAndSelf()
                } else {
                    ensureForeground(isRecording = false)
                }
                syncFloatingOverlayVisibility()
                return@launch
            }

            captureEngine = engine
            activeCallType = callType
            activeSampleRate = settings.sampleRateHz
            activeBitrate = settings.bitrateKbps
            recordingStartTimeMs = System.currentTimeMillis()

            _liveState.update {
                it.copy(
                    isMonitoring = true,
                    isRecording = true,
                    currentCallType = callType,
                    elapsedMs = 0L,
                    amplitude = 0,
                    activeFormat = engine.actualFormat,
                    activeSourceLabel = engine.actualSourceLabel,
                    errorMessage = null
                )
            }

            syncFloatingOverlayVisibility()
            startRecordingTimer(callType)
        }
    }

    private fun startRecordingTimer(callType: CallType) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && _liveState.value.isRecording) {
                val elapsed = System.currentTimeMillis() - recordingStartTimeMs
                _liveState.update { it.copy(elapsedMs = elapsed) }
                floatingButtonController?.updateState(isRecording = true, elapsedMs = elapsed)
                if (isForegroundActive) {
                    notificationManager.notify(
                        NOTIFICATION_ID_SERVICE,
                        buildRecordingNotification(callType, elapsed)
                    )
                }
                delay(500L)
            }
        }
    }

    private fun finishRecording() {
        delayedStartJob?.cancel()
        delayedStartJob = null
        timerJob?.cancel()
        timerJob = null

        val engine = captureEngine ?: run {
            serviceScope.launch {
                val settings = settingsRepository.getCurrentSettings()
                if (settings.autoRecordEnabled && settings.keepMonitorServiceAlive) {
                    ensureForeground(isRecording = false)
                } else {
                    stopForegroundAndSelf()
                }
                syncFloatingOverlayVisibility()
            }
            return
        }

        captureEngine = null
        val durationMs = (System.currentTimeMillis() - recordingStartTimeMs).coerceAtLeast(0L)
        val callType = activeCallType
        val sampleRate = activeSampleRate
        val bitrate = activeBitrate

        serviceScope.launch {
            val waveform = engine.stop()
            val file = engine.activeOutputFile
            val format = engine.actualFormat
            val sourceLabel = engine.actualSourceLabel
            val hadSignal = engine.hasObservedSignal

            restoreAudioState()
            releaseWakeLock()

            val settings = settingsRepository.getCurrentSettings()
            val minDurationMs = if (callType == CallType.MANUAL) 500L else settings.minDurationSeconds * 1000L

            var savedTitle: String? = null
            var silenceWarning: String? = null

            if (file.exists() && file.length() > 128L && durationMs >= minDurationMs) {
                val readableTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(recordingStartTimeMs))
                val title = when (callType) {
                    CallType.INCOMING -> "مکالمه ورودی ($readableTime)"
                    CallType.OUTGOING -> "مکالمه خروجی ($readableTime)"
                    CallType.MANUAL -> "ضبط دستی ($readableTime)"
                }

                val exportedUri = if (settings.autoExportToMusic) {
                    MediaStoreExporter.exportToMusicFolder(
                        context = applicationContext,
                        sourceFile = file,
                        displayTitle = title,
                        mimeType = format.mimeType
                    )
                } else {
                    null
                }

                val entity = RecordingEntity(
                    title = title,
                    callType = callType.id,
                    filePath = file.absolutePath,
                    fileName = file.name,
                    formatId = format.id,
                    mimeType = format.mimeType,
                    sampleRateHz = sampleRate,
                    bitrateKbps = bitrate,
                    audioSourceUsed = sourceLabel,
                    durationMs = durationMs,
                    fileSizeBytes = file.length(),
                    timestamp = recordingStartTimeMs,
                    isFavorite = false,
                    exportedUri = exportedUri,
                    waveformSamples = waveform.joinToString(",")
                )
                recordingRepository.insert(entity)
                recordingRepository.enforceStorageLimit(
                    limitBytes = settings.storageLimit.limitBytes,
                    autoDeleteOldest = settings.autoDeleteOldestOnLimit
                )
                savedTitle = title

                if (!hadSignal) {
                    silenceWarning = "هشدار: سیستم گوشی در پس‌زمینه ورودی صدا را مسدود کرد؛ از بخش دسترسی‌ها، «شروع خودکار» و «باتری بدون محدودیت» را برای سایبری فعال کنید."
                }

                if (settings.showNotifications) {
                    showSavedNotification(title, durationMs, hadSignal)
                }
            } else {
                runCatching { file.delete() }
            }

            _liveState.update {
                it.copy(
                    isRecording = false,
                    currentCallType = null,
                    elapsedMs = 0L,
                    amplitude = 0,
                    lastSavedTitle = savedTitle ?: it.lastSavedTitle,
                    errorMessage = silenceWarning
                )
            }

            syncFloatingOverlayVisibility()

            if (settings.autoRecordEnabled && settings.keepMonitorServiceAlive) {
                ensureForeground(isRecording = false)
                _liveState.update { it.copy(isMonitoring = true) }
            } else {
                _liveState.update { it.copy(isMonitoring = false) }
                if (!settings.floatingButtonAlwaysOn) {
                    floatingButtonController?.hide()
                }
                stopForegroundAndSelf()
            }
        }
    }

    private fun applyCallAudioBoostIfNeeded(
        callType: CallType,
        autoSpeaker: Boolean,
        twoWayBridgeBoost: Boolean
    ) {
        if (callType == CallType.MANUAL) return
        runCatching {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return

            if (twoWayBridgeBoost) {
                val maxCallVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                val currentCallVol = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)
                if (currentCallVol > 0 && maxCallVol > 0) {
                    val targetVol = (maxCallVol * 0.90f).toInt().coerceAtLeast(currentCallVol)
                    if (targetVol > currentCallVol) {
                        previousCallVolume = currentCallVol
                        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVol, 0)
                    }
                }
            }

            if (autoSpeaker) {
                previousAudioMode = audioManager.mode
                @Suppress("DEPRECATION")
                previousSpeakerState = audioManager.isSpeakerphoneOn

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val speaker = audioManager.availableCommunicationDevices.firstOrNull {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speaker != null) {
                        audioManager.setCommunicationDevice(speaker)
                    }
                }
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = true
            }
        }
    }

    private fun restoreAudioState() {
        runCatching {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            previousCallVolume?.let { prevVol ->
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, prevVol, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && previousSpeakerState != null) {
                audioManager.clearCommunicationDevice()
            }
            previousSpeakerState?.let {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = it
            }
            previousAudioMode?.let {
                audioManager.mode = it
            }
        }
        previousAudioMode = null
        previousSpeakerState = null
        previousCallVolume = null
    }

    private fun acquireWakeLock() {
        runCatching {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            if (wakeLock == null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Cyberi::CallRecordWakeLock")
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerTelephonyListenerIfPermitted() {
        if (telephonyRegistered) return
        val hasPhonePerm = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPhonePerm) return

        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        telephonyManager = tm

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleTelephonyState(state)
                }
            }
            telephonyCallback = callback
            val ok = runCatching {
                tm.registerTelephonyCallback(mainExecutor, callback)
                true
            }.getOrDefault(false)
            telephonyRegistered = ok
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    handleTelephonyState(state)
                }
            }
            legacyPhoneListener = listener
            @Suppress("DEPRECATION")
            val ok = runCatching {
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                true
            }.getOrDefault(false)
            telephonyRegistered = ok
        }
    }

    private fun handleTelephonyState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                isInTelephonyCall = true
                syncFloatingOverlayVisibility()
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                isInTelephonyCall = false
                syncFloatingOverlayVisibility()
            }
        }

        val transition = CallStateTracker.onTelephonyStateChanged(applicationContext, state)
        when (transition) {
            is CallTransition.StartRecording -> {
                serviceScope.launch {
                    val settings = settingsRepository.getCurrentSettings()
                    if (!settings.autoRecordEnabled) return@launch
                    val shouldRecord = when (transition.callType) {
                        CallType.INCOMING -> settings.recordIncomingCalls
                        CallType.OUTGOING -> settings.recordOutgoingCalls
                        CallType.MANUAL -> true
                    }
                    if (shouldRecord && hasMicPermission()) {
                        if (ensureForeground(isRecording = true, callType = transition.callType)) {
                            beginRecording(transition.callType)
                        }
                    }
                }
            }

            is CallTransition.StopRecording -> {
                if (_liveState.value.isRecording) {
                    finishRecording()
                }
            }

            CallTransition.None -> Unit
        }
    }

    private fun unregisterTelephonyListener() {
        if (!telephonyRegistered) return
        val tm = telephonyManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = telephonyCallback as? TelephonyCallback
            if (cb != null) {
                runCatching { tm.unregisterTelephonyCallback(cb) }
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = legacyPhoneListener
            if (listener != null) {
                @Suppress("DEPRECATION")
                runCatching { tm.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
        }
        telephonyRegistered = false
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val monitorChannel = NotificationChannel(
                CHANNEL_MONITORING,
                getString(R.string.notification_channel_monitoring_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = getString(R.string.notification_channel_monitoring_desc)
                setShowBadge(false)
            }

            val recordChannel = NotificationChannel(
                CHANNEL_RECORDING,
                getString(R.string.notification_channel_recording_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_recording_desc)
            }

            notificationManager.createNotificationChannel(monitorChannel)
            notificationManager.createNotificationChannel(recordChannel)
        }
    }

    private fun buildMonitoringNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val instantRecordIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, CallRecorderService::class.java).apply { action = ACTION_TOGGLE_INSTANT_RECORD },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_MONITORING)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(getString(R.string.monitoring_notif_title))
            .setContentText(getString(R.string.monitoring_notif_text))
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, "ضبط فوری", instantRecordIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun buildRecordingNotification(callType: CallType, elapsedMs: Long): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, CallRecorderService::class.java).apply { action = ACTION_STOP_RECORDING },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val durationText = DeviceCompatibilityManager.formatDuration(elapsedMs)
        return NotificationCompat.Builder(this, CHANNEL_RECORDING)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("${getString(R.string.recording_notif_title)} (${callType.label})")
            .setContentText("مدت زمان: $durationText • ضبط دوطرفه با وضوح بالا")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "توقف و ذخیره", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showSavedNotification(title: String, durationMs: Long, hadSignal: Boolean) {
        val openIntent = PendingIntent.getActivity(
            this,
            3,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val content = if (hadSignal) {
            "$title • مدت: ${DeviceCompatibilityManager.formatDuration(durationMs)}"
        } else {
            "هشدار بی‌صدا بودن ضبط! دسترسی باتری و شروع خودکار را برای سایبری باز کنید"
        }
        val notif = NotificationCompat.Builder(this, CHANNEL_RECORDING)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(if (hadSignal) getString(R.string.saved_notif_title) else "نیاز به دسترسی پس‌زمینه")
            .setContentText(content)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_SAVED, notif)
    }

    private fun stopForegroundAndSelf() {
        isForegroundActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        floatingButtonController?.hide()
        unregisterTelephonyListener()
        releaseWakeLock()
        restoreAudioState()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_MONITORING = "com.example.action.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.action.STOP_MONITORING"
        const val ACTION_START_CALL_RECORDING = "com.example.action.START_CALL_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.action.STOP_RECORDING"
        const val ACTION_SHOW_CALL_OVERLAY = "com.example.action.SHOW_CALL_OVERLAY"
        const val ACTION_HIDE_CALL_OVERLAY = "com.example.action.HIDE_CALL_OVERLAY"
        const val ACTION_SYNC_OVERLAY = "com.example.action.SYNC_OVERLAY"
        const val ACTION_TOGGLE_INSTANT_RECORD = "com.example.action.TOGGLE_INSTANT_RECORD"
        const val EXTRA_CALL_TYPE = "extra_call_type"

        private const val CHANNEL_MONITORING = "cyberi_monitoring_channel"
        private const val CHANNEL_RECORDING = "cyberi_recording_channel"
        private const val NOTIFICATION_ID_SERVICE = 1001
        private const val NOTIFICATION_ID_SAVED = 1002

        private val _liveState = MutableStateFlow(LiveRecorderState())
        val liveState: StateFlow<LiveRecorderState> = _liveState.asStateFlow()

        fun startMonitoring(context: Context) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            runCatching { context.startService(intent) }
        }

        fun startRecording(context: Context, callType: CallType) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_START_CALL_RECORDING
                putExtra(EXTRA_CALL_TYPE, callType.id)
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            runCatching { context.startService(intent) }
        }

        fun showCallOverlay(context: Context) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_SHOW_CALL_OVERLAY
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        fun hideCallOverlay(context: Context) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_HIDE_CALL_OVERLAY
            }
            runCatching { context.startService(intent) }
        }

        fun syncOverlay(context: Context) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_SYNC_OVERLAY
            }
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }
    }
}
