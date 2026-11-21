package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.model.AudioFormatOption
import com.example.model.AudioSourceOption
import com.example.model.CallType
import com.example.model.RecordingSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.roundToInt

class CallAudioCaptureEngine(
    private val context: Context,
    private val outputFile: File,
    private val settings: RecordingSettings,
    private val callType: CallType = CallType.MANUAL,
    private val onAmplitude: (Int) -> Unit,
    private val onSourceChanged: (AudioFormatOption, String) -> Unit = { _, _ -> }
) {
    private val stateMutex = Mutex()
    private var mediaRecorder: MediaRecorder? = null
    private var wavRecorder: WavAudioRecorder? = null
    private var meterJob: Job? = null
    private val waveformCollector = mutableListOf<Int>()
    private var isStopped = false

    var actualFormat: AudioFormatOption = settings.audioFormat
        private set
    var actualSourceLabel: String = settings.audioSource.title
        private set
    var activeOutputFile: File = outputFile
        private set

    @Volatile
    var hasObservedSignal: Boolean = false
        private set

    fun start(scope: CoroutineScope): Boolean {
        outputFile.parentFile?.mkdirs()

        if (settings.audioFormat == AudioFormatOption.WAV_PCM) {
            val wav = WavAudioRecorder(outputFile, settings, callType, onAmplitude)
            if (wav.start(scope)) {
                wavRecorder = wav
                actualFormat = AudioFormatOption.WAV_PCM
                actualSourceLabel = wav.activeSourceLabel
                activeOutputFile = outputFile
                onSourceChanged(actualFormat, actualSourceLabel)
                return true
            }
        }

        val effectiveFormat = if (settings.audioFormat.isSupportedOnDevice()) {
            settings.audioFormat
        } else {
            AudioFormatOption.M4A_AAC
        }

        val sourceCandidates = WavAudioRecorder.buildSourcePriorityList(settings.audioSource, callType)
        for (idx in sourceCandidates.indices) {
            val (sourceInt, label) = sourceCandidates[idx]
            if (tryStartMediaRecorder(effectiveFormat, sourceInt, outputFile)) {
                actualFormat = effectiveFormat
                actualSourceLabel = label
                activeOutputFile = outputFile
                onSourceChanged(actualFormat, actualSourceLabel)
                startAmplitudePollingWithAutoSwap(
                    scope = scope,
                    format = effectiveFormat,
                    candidates = sourceCandidates,
                    startIndex = idx
                )
                return true
            }
        }

        // Ultimate fallback: M4A_AAC on MIC or WAV AudioRecord
        if (effectiveFormat != AudioFormatOption.M4A_AAC) {
            val fallbackM4a = File(outputFile.parentFile, outputFile.nameWithoutExtension + ".m4a")
            if (tryStartMediaRecorder(AudioFormatOption.M4A_AAC, MediaRecorder.AudioSource.MIC, fallbackM4a)) {
                actualFormat = AudioFormatOption.M4A_AAC
                actualSourceLabel = "میکروفون اصلی (جایگزین)"
                activeOutputFile = fallbackM4a
                onSourceChanged(actualFormat, actualSourceLabel)
                startAmplitudePollingWithAutoSwap(
                    scope = scope,
                    format = AudioFormatOption.M4A_AAC,
                    candidates = listOf(MediaRecorder.AudioSource.MIC to actualSourceLabel),
                    startIndex = 0
                )
                return true
            }
        }

        val wavFallbackFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + ".wav")
        val wav = WavAudioRecorder(wavFallbackFile, settings, callType, onAmplitude)
        if (wav.start(scope)) {
            wavRecorder = wav
            actualFormat = AudioFormatOption.WAV_PCM
            actualSourceLabel = wav.activeSourceLabel
            activeOutputFile = wavFallbackFile
            onSourceChanged(actualFormat, actualSourceLabel)
            return true
        }

        return false
    }

    private fun tryStartMediaRecorder(
        format: AudioFormatOption,
        audioSource: Int,
        targetFile: File
    ): Boolean {
        var recorder: MediaRecorder? = null
        return try {
            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) targetFile.delete()
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(audioSource)
                configureFormatAndEncoder(this, format)
                setOutputFile(targetFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            true
        } catch (_: Throwable) {
            runCatching { recorder?.reset() }
            runCatching { recorder?.release() }
            runCatching { if (targetFile.exists() && targetFile.length() == 0L) targetFile.delete() }
            false
        }
    }

    private fun configureFormatAndEncoder(recorder: MediaRecorder, format: AudioFormatOption) {
        val channels = if (settings.stereoChannels && format != AudioFormatOption.AMR_NB && format != AudioFormatOption.AMR_WB) 2 else 1
        when (format) {
            AudioFormatOption.M4A_AAC -> {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                recorder.setAudioChannels(channels)
                recorder.setAudioSamplingRate(settings.sampleRateHz.coerceIn(16000, 48000))
                recorder.setAudioEncodingBitRate(settings.bitrateKbps * 1000)
            }
            AudioFormatOption.OGG_OPUS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.OGG)
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                    recorder.setAudioChannels(channels)
                    recorder.setAudioSamplingRate(48000)
                    recorder.setAudioEncodingBitRate(settings.bitrateKbps * 1000)
                } else {
                    recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    recorder.setAudioChannels(channels)
                    recorder.setAudioSamplingRate(settings.sampleRateHz)
                    recorder.setAudioEncodingBitRate(settings.bitrateKbps * 1000)
                }
            }
            AudioFormatOption.THREE_GP_AMR_WB -> {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                recorder.setAudioChannels(1)
                recorder.setAudioSamplingRate(16000)
                recorder.setAudioEncodingBitRate(23850)
            }
            AudioFormatOption.WEBM_OPUS -> {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                    recorder.setAudioSamplingRate(48000)
                } else {
                    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.VORBIS)
                    recorder.setAudioSamplingRate(settings.sampleRateHz.coerceIn(16000, 48000))
                }
                recorder.setAudioChannels(channels)
                recorder.setAudioEncodingBitRate(settings.bitrateKbps * 1000)
            }
            AudioFormatOption.AMR_WB -> {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB)
                recorder.setAudioChannels(1)
                recorder.setAudioSamplingRate(16000)
                recorder.setAudioEncodingBitRate(23850)
            }
            AudioFormatOption.AMR_NB -> {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                recorder.setAudioChannels(1)
                recorder.setAudioSamplingRate(8000)
                recorder.setAudioEncodingBitRate(12200)
            }
            AudioFormatOption.WAV_PCM -> {
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }
        }
    }

    /**
     * Polls MediaRecorder amplitude and automatically hot-swaps to the next audio source
     * if Android's Audio HAL mutes the active source (22 consecutive zero-amplitude checks ~ 2.2s).
     */
    private fun startAmplitudePollingWithAutoSwap(
        scope: CoroutineScope,
        format: AudioFormatOption,
        candidates: List<Pair<Int, String>>,
        startIndex: Int
    ) {
        meterJob?.cancel()
        meterJob = scope.launch(Dispatchers.IO) {
            var currentIndex = startIndex
            var silentPollsForCurrentSource = 0
            var currentSourceHasSignal = false

            while (isActive && !isStopped) {
                val amp = stateMutex.withLock {
                    if (isStopped || mediaRecorder == null) -1
                    else runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                }
                if (amp < 0) break

                if (amp > 0) {
                    hasObservedSignal = true
                    currentSourceHasSignal = true
                    silentPollsForCurrentSource = 0
                } else if (!currentSourceHasSignal) {
                    silentPollsForCurrentSource++
                }

                val normalized = ((amp / 32767f) * 100f).roundToInt().coerceIn(0, 100)
                onAmplitude(normalized)
                waveformCollector.add(normalized)

                // If Android HAL silently zeroes out the current source for ~1.6 seconds, hot-swap!
                if (!currentSourceHasSignal && silentPollsForCurrentSource >= 16) {
                    val switched = stateMutex.withLock {
                        if (isStopped) HotSwapResult()
                        else tryHotSwapToNextSource(scope, format, candidates, currentIndex)
                    }
                    if (switched.nextIndex >= 0) {
                        currentIndex = switched.nextIndex
                        silentPollsForCurrentSource = 0
                        currentSourceHasSignal = false
                        waveformCollector.clear()
                    } else if (switched.switchedToWav) {
                        // WavAudioRecorder has taken over capture & amplitude callbacks
                        break
                    } else {
                        // All candidates already exhausted; avoid looping hot-swaps
                        silentPollsForCurrentSource = -100000
                    }
                }

                delay(100L)
            }
        }
    }

    private data class HotSwapResult(
        val nextIndex: Int = -1,
        val switchedToWav: Boolean = false
    )

    private fun tryHotSwapToNextSource(
        scope: CoroutineScope,
        format: AudioFormatOption,
        candidates: List<Pair<Int, String>>,
        currentIndex: Int
    ): HotSwapResult {
        for (nextIdx in (currentIndex + 1) until candidates.size) {
            val (nextSource, nextLabel) = candidates[nextIdx]
            stopAndReleaseCurrentMediaRecorder()
            if (tryStartMediaRecorder(format, nextSource, activeOutputFile)) {
                actualFormat = format
                actualSourceLabel = nextLabel
                onSourceChanged(actualFormat, actualSourceLabel)
                return HotSwapResult(nextIndex = nextIdx)
            }
        }

        // Last resort if all MediaRecorder sources produced pure zeros: switch to AudioRecord WAV
        stopAndReleaseCurrentMediaRecorder()
        runCatching { if (activeOutputFile.exists()) activeOutputFile.delete() }
        val wavFile = File(outputFile.parentFile, outputFile.nameWithoutExtension + ".wav")
        val wav = WavAudioRecorder(wavFile, settings, callType, onAmplitude)
        if (wav.start(scope)) {
            wavRecorder = wav
            actualFormat = AudioFormatOption.WAV_PCM
            actualSourceLabel = wav.activeSourceLabel
            activeOutputFile = wavFile
            onSourceChanged(actualFormat, actualSourceLabel)
            return HotSwapResult(switchedToWav = true)
        }

        // Could not hot-swap; restart original format on MIC so recording file remains valid
        val fallbackIdx = candidates.indexOfFirst { it.first == MediaRecorder.AudioSource.MIC }
            .takeIf { it >= 0 } ?: currentIndex
        val (src, lbl) = candidates[fallbackIdx]
        tryStartMediaRecorder(format, src, activeOutputFile)
        actualSourceLabel = lbl
        return HotSwapResult()
    }

    private fun stopAndReleaseCurrentMediaRecorder() {
        mediaRecorder?.let { rec ->
            runCatching { rec.stop() }
            runCatching { rec.reset() }
            runCatching { rec.release() }
        }
        mediaRecorder = null
    }

    suspend fun stop(): List<Int> {
        stateMutex.withLock {
            isStopped = true
        }
        meterJob?.cancel()
        meterJob = null

        wavRecorder?.let { wav ->
            val peaks = wav.stopAndGetWaveform()
            hasObservedSignal = hasObservedSignal || wav.hasObservedSignal
            wavRecorder = null
            return peaks
        }

        stateMutex.withLock {
            stopAndReleaseCurrentMediaRecorder()
        }
        return WavAudioRecorder.downsampleWaveform(waveformCollector)
    }
}
