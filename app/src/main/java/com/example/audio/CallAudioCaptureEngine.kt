package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.example.model.AudioFormatOption
import com.example.model.AudioSourceOption
import com.example.model.RecordingSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

class CallAudioCaptureEngine(
    private val context: Context,
    private val outputFile: File,
    private val settings: RecordingSettings,
    private val onAmplitude: (Int) -> Unit
) {
    private var mediaRecorder: MediaRecorder? = null
    private var wavRecorder: WavAudioRecorder? = null
    private var meterJob: Job? = null
    private val waveformCollector = mutableListOf<Int>()

    var actualFormat: AudioFormatOption = settings.audioFormat
        private set
    var actualSourceLabel: String = settings.audioSource.title
        private set

    fun start(scope: CoroutineScope): Boolean {
        outputFile.parentFile?.mkdirs()

        // WAV uses raw AudioRecord pipeline with hardware DSP & software gain
        if (settings.audioFormat == AudioFormatOption.WAV_PCM) {
            val wav = WavAudioRecorder(outputFile, settings, onAmplitude)
            if (wav.start(scope)) {
                wavRecorder = wav
                actualFormat = AudioFormatOption.WAV_PCM
                actualSourceLabel = wav.activeSourceLabel
                return true
            }
        }

        // Otherwise try configured compressed format with automatic smart source fallback
        val effectiveFormat = if (settings.audioFormat.isSupportedOnDevice()) {
            settings.audioFormat
        } else {
            AudioFormatOption.M4A_AAC
        }

        val sourceCandidates = buildMediaRecorderSourceCandidates(settings.audioSource)
        for ((sourceInt, label) in sourceCandidates) {
            if (tryStartMediaRecorder(effectiveFormat, sourceInt)) {
                actualFormat = effectiveFormat
                actualSourceLabel = label
                startAmplitudePolling(scope)
                return true
            }
        }

        // Ultimate fallback: M4A_AAC on MIC so recording never fails silently
        if (effectiveFormat != AudioFormatOption.M4A_AAC) {
            val fallbackFile = File(
                outputFile.parentFile,
                outputFile.nameWithoutExtension + ".m4a"
            )
            if (tryStartMediaRecorder(AudioFormatOption.M4A_AAC, MediaRecorder.AudioSource.MIC, fallbackFile)) {
                actualFormat = AudioFormatOption.M4A_AAC
                actualSourceLabel = "میکروفون جایگزین"
                startAmplitudePolling(scope)
                return true
            }
        }
        return false
    }

    private fun tryStartMediaRecorder(
        format: AudioFormatOption,
        audioSource: Int,
        targetFile: File = outputFile
    ): Boolean {
        var recorder: MediaRecorder? = null
        return try {
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
                // Handled by WavAudioRecorder; fallback here if ever called
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            }
        }
    }

    private fun startAmplitudePolling(scope: CoroutineScope) {
        meterJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                val amp = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                val normalized = ((amp / 32767f) * 100f).roundToInt().coerceIn(0, 100)
                onAmplitude(normalized)
                waveformCollector.add(normalized)
                delay(120L)
            }
        }
    }

    suspend fun stop(): List<Int> {
        wavRecorder?.let { wav ->
            val peaks = wav.stopAndGetWaveform()
            wavRecorder = null
            return peaks
        }
        meterJob?.cancel()
        meterJob = null
        mediaRecorder?.let { rec ->
            runCatching { rec.stop() }
            runCatching { rec.reset() }
            runCatching { rec.release() }
        }
        mediaRecorder = null
        return WavAudioRecorder.downsampleWaveform(waveformCollector)
    }

    private fun buildMediaRecorderSourceCandidates(selected: AudioSourceOption): List<Pair<Int, String>> {
        return when (selected) {
            AudioSourceOption.AUTO_SMART -> listOf(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION to "ارتباط صوتی هوشمند",
                MediaRecorder.AudioSource.VOICE_RECOGNITION to "حساسیت بالا (هوشمند)",
                MediaRecorder.AudioSource.MIC to "میکروفون اصلی"
            )
            AudioSourceOption.VOICE_CALL -> listOf(
                MediaRecorder.AudioSource.VOICE_CALL to "خط مستقیم تماس",
                MediaRecorder.AudioSource.VOICE_COMMUNICATION to "ارتباط صوتی (جایگزین خودکار)",
                MediaRecorder.AudioSource.MIC to "میکروفون اصلی"
            )
            AudioSourceOption.VOICE_COMMUNICATION -> listOf(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION to "ارتباط صوتی دوطرفه",
                MediaRecorder.AudioSource.MIC to "میکروفون اصلی"
            )
            AudioSourceOption.VOICE_RECOGNITION -> listOf(
                MediaRecorder.AudioSource.VOICE_RECOGNITION to "حساسیت بالا (بلندگو)",
                MediaRecorder.AudioSource.MIC to "میکروفون اصلی"
            )
            AudioSourceOption.MIC -> listOf(
                MediaRecorder.AudioSource.MIC to "میکروفون استاندارد"
            )
        }
    }
}
