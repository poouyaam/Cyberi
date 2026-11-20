package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import com.example.model.AudioSourceOption
import com.example.model.RecordingSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

class WavAudioRecorder(
    private val outputFile: File,
    private val settings: RecordingSettings,
    private val onAmplitude: (Int) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var autoGainControl: AutomaticGainControl? = null
    private var recordJob: Job? = null

    @Volatile
    private var isRecording = false
    var activeSourceLabel: String = settings.audioSource.title
        private set

    private val waveformCollector = mutableListOf<Int>()

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope): Boolean {
        val channelConfig = if (settings.stereoChannels) {
            AudioFormat.CHANNEL_IN_STEREO
        } else {
            AudioFormat.CHANNEL_IN_MONO
        }
        val channelCount = if (settings.stereoChannels) 2 else 1
        val sampleRate = settings.sampleRateHz.coerceIn(16000, 48000)
        val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            channelConfig,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufSize <= 0) return false
        val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

        val candidates = buildSourcePriorityList(settings.audioSource)
        var initializedRecord: AudioRecord? = null
        var chosenSourceLabel = settings.audioSource.title

        for ((sourceInt, label) in candidates) {
            try {
                val candidate = AudioRecord(
                    sourceInt,
                    sampleRate,
                    channelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    initializedRecord = candidate
                    chosenSourceLabel = label
                    break
                } else {
                    candidate.release()
                }
            } catch (_: Throwable) {
                // Try next audio source candidate
            }
        }

        val record = initializedRecord ?: return false
        audioRecord = record
        activeSourceLabel = chosenSourceLabel
        attachAudioEffects(record.audioSessionId)

        isRecording = true
        record.startRecording()

        recordJob = scope.launch(Dispatchers.IO) {
            outputFile.parentFile?.mkdirs()
            RandomAccessFile(outputFile, "rw").use { raf ->
                // Write placeholder 44-byte WAV header
                raf.setLength(0)
                raf.write(ByteArray(44))

                val buffer = ShortArray(bufferSize / 2)
                val byteBuffer = ByteArray(buffer.size * 2)
                val gainFactor = (settings.micGainPercent.coerceIn(100, 250)) / 100f
                var totalBytesWritten = 0L
                var peakAccumulator = 0
                var chunkCounter = 0

                while (isActive && isRecording) {
                    val readCount = record.read(buffer, 0, buffer.size)
                    if (readCount > 0) {
                        var maxSample = 0
                        for (i in 0 until readCount) {
                            val boosted = (buffer[i] * gainFactor).roundToInt()
                                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            val s = boosted.toShort()
                            buffer[i] = s
                            val absVal = abs(s.toInt())
                            if (absVal > maxSample) maxSample = absVal
                            byteBuffer[i * 2] = (s.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((s.toInt() shr 8) and 0xFF).toByte()
                        }
                        raf.write(byteBuffer, 0, readCount * 2)
                        totalBytesWritten += readCount * 2L

                        val normalized = ((maxSample / 32767f) * 100f).roundToInt().coerceIn(0, 100)
                        onAmplitude(normalized)
                        peakAccumulator = maxOf(peakAccumulator, normalized)
                        chunkCounter++
                        if (chunkCounter >= 3) {
                            waveformCollector.add(peakAccumulator)
                            peakAccumulator = 0
                            chunkCounter = 0
                        }
                    }
                }

                // Finalize RIFF/WAV header
                writeWavHeader(
                    raf = raf,
                    totalAudioBytes = totalBytesWritten,
                    sampleRate = sampleRate,
                    channels = channelCount,
                    bitsPerSample = 16
                )
            }
        }
        return true
    }

    suspend fun stopAndGetWaveform(): List<Int> {
        isRecording = false
        try {
            audioRecord?.let { rec ->
                if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    rec.stop()
                }
            }
        } catch (_: Throwable) {
        }
        recordJob?.join()
        releaseResources()
        return downsampleWaveform(waveformCollector)
    }

    private fun releaseResources() {
        runCatching { noiseSuppressor?.release() }
        runCatching { echoCanceler?.release() }
        runCatching { autoGainControl?.release() }
        runCatching { audioRecord?.release() }
        noiseSuppressor = null
        echoCanceler = null
        autoGainControl = null
        audioRecord = null
    }

    private fun attachAudioEffects(sessionId: Int) {
        if (settings.noiseSuppression && NoiseSuppressor.isAvailable()) {
            runCatching {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
            }
        }
        if (settings.echoCancellation && AcousticEchoCanceler.isAvailable()) {
            runCatching {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
            }
        }
        if (settings.autoGainControl && AutomaticGainControl.isAvailable()) {
            runCatching {
                autoGainControl = AutomaticGainControl.create(sessionId)?.apply { enabled = true }
            }
        }
    }

    private fun writeWavHeader(
        raf: RandomAccessFile,
        totalAudioBytes: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate.toLong() * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = totalAudioBytes + 36

        val header = ByteArray(44)
        // RIFF chunk
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen.toInt())
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        // fmt subchunk
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16) // PCM chunk size
        header[20] = 1
        header[21] = 0 // PCM format = 1
        header[22] = channels.toByte()
        header[23] = 0
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate.toInt())
        header[32] = blockAlign.toByte()
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        // data subchunk
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, totalAudioBytes.toInt())

        raf.seek(0)
        raf.write(header, 0, 44)
    }

    private fun writeIntLE(buffer: ByteArray, offset: Int, value: Int) {
        buffer[offset] = (value and 0xFF).toByte()
        buffer[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buffer[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buffer[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    companion object {
        fun buildSourcePriorityList(selected: AudioSourceOption): List<Pair<Int, String>> {
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

        fun downsampleWaveform(raw: List<Int>, targetBars: Int = 36): List<Int> {
            if (raw.isEmpty()) return List(targetBars) { 12 }
            if (raw.size <= targetBars) return raw
            val step = raw.size.toFloat() / targetBars
            return List(targetBars) { index ->
                val start = (index * step).toInt()
                val end = min(((index + 1) * step).toInt(), raw.size).coerceAtLeast(start + 1)
                var maxVal = 0
                for (i in start until end) {
                    if (raw[i] > maxVal) maxVal = raw[i]
                }
                maxVal.coerceIn(6, 100)
            }
        }
    }
}
