package com.example.audio

import android.media.MediaPlayer
import android.media.PlaybackParams
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
    val errorMessage: String? = null
)

class AudioPlaybackManager(private val scope: CoroutineScope) {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

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
        if (!file.exists()) {
            _state.update { it.copy(errorMessage = "فایل صوتی یافت نشد") }
            return
        }
        runCatching {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                playbackParams = PlaybackParams().setSpeed(_state.value.speed)
                setOnCompletionListener {
                    _state.update {
                        it.copy(isPlaying = false, positionMs = 0L)
                    }
                    runCatching { seekTo(0) }
                }
                start()
            }
            mediaPlayer = player
            _state.update {
                it.copy(
                    activeRecordingId = recording.id,
                    activeTitle = recording.title,
                    isPlaying = true,
                    positionMs = 0L,
                    durationMs = player.duration.toLong().coerceAtLeast(recording.durationMs),
                    errorMessage = null
                )
            }
            startProgressUpdater()
        }.onFailure {
            _state.update { it.copy(errorMessage = "خطا در پخش این فرمت صوتی") }
        }
    }

    fun pause() {
        runCatching { mediaPlayer?.pause() }
        _state.update { it.copy(isPlaying = false) }
    }

    fun resume() {
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
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        _state.update {
            PlaybackUiState(speed = it.speed)
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
