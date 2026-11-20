package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AudioFormatOption
import com.example.model.AudioSourceOption
import com.example.model.RecordingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "cyberi_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val AUTO_RECORD_ENABLED = booleanPreferencesKey("auto_record_enabled")
        val RECORD_INCOMING = booleanPreferencesKey("record_incoming")
        val RECORD_OUTGOING = booleanPreferencesKey("record_outgoing")
        val AUDIO_FORMAT = stringPreferencesKey("audio_format")
        val AUDIO_SOURCE = stringPreferencesKey("audio_source")
        val SAMPLE_RATE_HZ = intPreferencesKey("sample_rate_hz")
        val BITRATE_KBPS = intPreferencesKey("bitrate_kbps")
        val STEREO_CHANNELS = booleanPreferencesKey("stereo_channels")
        val NOISE_SUPPRESSION = booleanPreferencesKey("noise_suppression")
        val AUTO_GAIN_CONTROL = booleanPreferencesKey("auto_gain_control")
        val ECHO_CANCELLATION = booleanPreferencesKey("echo_cancellation")
        val MIC_GAIN_PERCENT = intPreferencesKey("mic_gain_percent")
        val AUTO_SPEAKER_BOOST = booleanPreferencesKey("auto_speaker_boost")
        val START_DELAY_SECONDS = intPreferencesKey("start_delay_seconds")
        val MIN_DURATION_SECONDS = intPreferencesKey("min_duration_seconds")
        val AUTO_EXPORT_MUSIC = booleanPreferencesKey("auto_export_music")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val KEEP_MONITOR_ALIVE = booleanPreferencesKey("keep_monitor_alive")
    }

    val settingsFlow: Flow<RecordingSettings> = context.settingsDataStore.data.map { prefs ->
        RecordingSettings(
            autoRecordEnabled = prefs[Keys.AUTO_RECORD_ENABLED] ?: true,
            recordIncomingCalls = prefs[Keys.RECORD_INCOMING] ?: true,
            recordOutgoingCalls = prefs[Keys.RECORD_OUTGOING] ?: true,
            audioFormat = AudioFormatOption.fromId(prefs[Keys.AUDIO_FORMAT]),
            audioSource = AudioSourceOption.fromId(prefs[Keys.AUDIO_SOURCE]),
            sampleRateHz = prefs[Keys.SAMPLE_RATE_HZ] ?: 48000,
            bitrateKbps = prefs[Keys.BITRATE_KBPS] ?: 192,
            stereoChannels = prefs[Keys.STEREO_CHANNELS] ?: false,
            noiseSuppression = prefs[Keys.NOISE_SUPPRESSION] ?: true,
            autoGainControl = prefs[Keys.AUTO_GAIN_CONTROL] ?: true,
            echoCancellation = prefs[Keys.ECHO_CANCELLATION] ?: true,
            micGainPercent = prefs[Keys.MIC_GAIN_PERCENT] ?: 140,
            autoSpeakerBoost = prefs[Keys.AUTO_SPEAKER_BOOST] ?: false,
            startDelaySeconds = prefs[Keys.START_DELAY_SECONDS] ?: 0,
            minDurationSeconds = prefs[Keys.MIN_DURATION_SECONDS] ?: 2,
            autoExportToMusic = prefs[Keys.AUTO_EXPORT_MUSIC] ?: true,
            showNotifications = prefs[Keys.SHOW_NOTIFICATIONS] ?: true,
            keepMonitorServiceAlive = prefs[Keys.KEEP_MONITOR_ALIVE] ?: true
        )
    }

    suspend fun getCurrentSettings(): RecordingSettings = settingsFlow.first()

    suspend fun updateSettings(transform: (RecordingSettings) -> RecordingSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = RecordingSettings(
                autoRecordEnabled = prefs[Keys.AUTO_RECORD_ENABLED] ?: true,
                recordIncomingCalls = prefs[Keys.RECORD_INCOMING] ?: true,
                recordOutgoingCalls = prefs[Keys.RECORD_OUTGOING] ?: true,
                audioFormat = AudioFormatOption.fromId(prefs[Keys.AUDIO_FORMAT]),
                audioSource = AudioSourceOption.fromId(prefs[Keys.AUDIO_SOURCE]),
                sampleRateHz = prefs[Keys.SAMPLE_RATE_HZ] ?: 48000,
                bitrateKbps = prefs[Keys.BITRATE_KBPS] ?: 192,
                stereoChannels = prefs[Keys.STEREO_CHANNELS] ?: false,
                noiseSuppression = prefs[Keys.NOISE_SUPPRESSION] ?: true,
                autoGainControl = prefs[Keys.AUTO_GAIN_CONTROL] ?: true,
                echoCancellation = prefs[Keys.ECHO_CANCELLATION] ?: true,
                micGainPercent = prefs[Keys.MIC_GAIN_PERCENT] ?: 140,
                autoSpeakerBoost = prefs[Keys.AUTO_SPEAKER_BOOST] ?: false,
                startDelaySeconds = prefs[Keys.START_DELAY_SECONDS] ?: 0,
                minDurationSeconds = prefs[Keys.MIN_DURATION_SECONDS] ?: 2,
                autoExportToMusic = prefs[Keys.AUTO_EXPORT_MUSIC] ?: true,
                showNotifications = prefs[Keys.SHOW_NOTIFICATIONS] ?: true,
                keepMonitorServiceAlive = prefs[Keys.KEEP_MONITOR_ALIVE] ?: true
            )
            val updated = transform(current)
            prefs[Keys.AUTO_RECORD_ENABLED] = updated.autoRecordEnabled
            prefs[Keys.RECORD_INCOMING] = updated.recordIncomingCalls
            prefs[Keys.RECORD_OUTGOING] = updated.recordOutgoingCalls
            prefs[Keys.AUDIO_FORMAT] = updated.audioFormat.id
            prefs[Keys.AUDIO_SOURCE] = updated.audioSource.id
            prefs[Keys.SAMPLE_RATE_HZ] = updated.sampleRateHz
            prefs[Keys.BITRATE_KBPS] = updated.bitrateKbps
            prefs[Keys.STEREO_CHANNELS] = updated.stereoChannels
            prefs[Keys.NOISE_SUPPRESSION] = updated.noiseSuppression
            prefs[Keys.AUTO_GAIN_CONTROL] = updated.autoGainControl
            prefs[Keys.ECHO_CANCELLATION] = updated.echoCancellation
            prefs[Keys.MIC_GAIN_PERCENT] = updated.micGainPercent
            prefs[Keys.AUTO_SPEAKER_BOOST] = updated.autoSpeakerBoost
            prefs[Keys.START_DELAY_SECONDS] = updated.startDelaySeconds
            prefs[Keys.MIN_DURATION_SECONDS] = updated.minDurationSeconds
            prefs[Keys.AUTO_EXPORT_MUSIC] = updated.autoExportToMusic
            prefs[Keys.SHOW_NOTIFICATIONS] = updated.showNotifications
            prefs[Keys.KEEP_MONITOR_ALIVE] = updated.keepMonitorServiceAlive
        }
    }
}
