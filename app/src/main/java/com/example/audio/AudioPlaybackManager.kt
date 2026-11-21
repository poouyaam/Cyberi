package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import com.example.data.db.RecordingEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackUiState(
    val activeRecordingId: Long? = null,
    val activeTitle: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val isBoostEnabled: Boolean = true,
    val errorMessage: String? = null
)

class AudioPlaybackManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var progressJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    fun togglePlay(recording: RecordingEntity) {
        val current = _state.value
        if (current.activeRecordingId == recording.id && mediaPlayer != null) {
            if (current.isPlaying) {
                pause()
            } else {
                resume()
            }
            return
        }
        startNew(recording)
    }

    private fun startNew(recording: RecordingEntity) {
        stop()
        val file = File(recording.filePath)
        if (!file.exists() || file.length() <= 64L) {
            _state.update { it.copy(errorMessage = "فایل صوتی یافت نشد یا خالی است") }
            return
        }

        val isAllFlatSilence = recording.waveformSamples
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .let { bars -> bars.isNotEmpty() && bars.all { it <= 6 } }
        val mediaVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 1
        val warningHint = when {
            mediaVolume == 0 -> "صدای رسانه گوشی قطع است؛ کلید افزایش صدا را بزنید"
            isAllFlatSilence -> "این فایل فاقد سیگنال صوتی است (در ضبط قبلی صدا مسدود شده بود)"
            else -> null
        }

        requestFocus()

        runCatching {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                runCatching {
                    playbackParams = PlaybackParams().setSpeed(_state.value.speed)
                }
                setOnCompletionListener {
                    _state.update {
                        it.copy(isPlaying = false, positionMs = 0L)
                    }
                    runCatching { seekTo(0) }
                }
                setOnErrorListener { _, _, _ ->
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            errorMessage = "خطا در پخش این فایل صوتی"
                        )
                    }
                    true
                }
                start()
            }
            mediaPlayer = player
            attachLoudnessEnhancer(player.audioSessionId, _state.value.isBoostEnabled)

            _state.update {
                it.copy(
                    activeRecordingId = recording.id,
                    activeTitle = recording.title,
                    isPlaying = true,
                    positionMs = 0L,
                    durationMs = player.duration.toLong().coerceAtLeast(recording.durationMs),
                    errorMessage = warningHint
                )
            }
            startProgressUpdater()
        }.onFailure {
            abandonFocus()
            _state.update { it.copy(errorMessage = "خطا در پخش این فرمت صوتی") }
        }
    }

    private fun attachLoudnessEnhancer(sessionId: Int, enabled: Boolean) {
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = runCatching {
            LoudnessEnhancer(sessionId).apply {
                setTargetGain(if (enabled) 900 else 0) // +9 dB clear speech boost
                this.enabled = enabled
            }
        }.getOrNull()
    }

    fun toggleBoost() {
        val next = !_state.value.isBoostEnabled
        _state.update { it.copy(isBoostEnabled = next) }
        runCatching {
            loudnessEnhancer?.apply {
                setTargetGain(if (next) 900 else 0)
                enabled = next
            }
        }
    }

    fun pause() {
        runCatching { mediaPlayer?.pause() }
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
        requestFocus()
        runCatching {
            mediaPlayer?.start()
            _state.update { it.copy(isPlaying = true) }
        }
    }

    fun seekTo(positionMs: Long) {
        runCatching {
            mediaPlayer?.seekTo(positionMs.toInt())
            _state.update { it.copy(positionMs = positionMs) }
        }
    }

    fun cycleSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 2.0f, 0.75f)
        val currentIdx = speeds.indexOf(_state.value.speed)
        val nextSpeed = speeds[(currentIdx + 1) % speeds.size]
        _state.update { it.copy(speed = nextSpeed) }
        runCatching {
            mediaPlayer?.let { player ->
                val wasPlaying = player.isPlaying
                player.playbackParams = PlaybackParams().setSpeed(nextSpeed)
                if (!wasPlaying) player.pause()
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        abandonFocus()
        _state.update {
            PlaybackUiState(speed = it.speed, isBoostEnabled = it.isBoostEnabled)
        }
    }

    private fun requestFocus() {
        val am = audioManager ?: return
        runCatching {
            if (am.mode == AudioManager.MODE_IN_COMMUNICATION) {
                am.mode = AudioManager.MODE_NORMAL
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            }
        }
    }

    private fun abandonFocus() {
        val am = audioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _state.update {
                            it.copy(
                                positionMs = player.currentPosition.toLong(),
                                durationMs = player.duration.toLong().coerceAtLeast(it.durationMs)
                            )
                        }
                    }
                }
                delay(150L)
            }
        }
    }
}
