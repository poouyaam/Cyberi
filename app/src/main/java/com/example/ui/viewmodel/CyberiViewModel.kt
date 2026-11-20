package com.example.ui.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.CyberiApplication
import com.example.audio.AudioPlaybackManager
import com.example.audio.MediaStoreExporter
import com.example.audio.PlaybackUiState
import com.example.data.db.RecordingEntity
import com.example.model.AudioFormatOption
import com.example.model.AudioSourceOption
import com.example.model.CallType
import com.example.model.RecordingSettings
import com.example.service.CallRecorderService
import com.example.service.LiveRecorderState
import com.example.util.DeviceCompatibilityManager
import com.example.util.DeviceCompatibilityStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class RecordingFilter(val label: String) {
    ALL("همه"),
    INCOMING("ورودی"),
    OUTGOING("خروجی"),
    MANUAL("آزمایشی"),
    FAVORITES("نشان‌شده")
}

class CyberiViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as CyberiApplication
    private val recordingRepository = app.recordingRepository
    private val settingsRepository = app.settingsRepository
    private val playbackManager = AudioPlaybackManager(viewModelScope)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(RecordingFilter.ALL)
    val selectedFilter: StateFlow<RecordingFilter> = _selectedFilter.asStateFlow()

    private val _deviceStatus = MutableStateFlow(DeviceCompatibilityManager.inspectDevice(app))
    val deviceStatus: StateFlow<DeviceCompatibilityStatus> = _deviceStatus.asStateFlow()

    val settings: StateFlow<RecordingSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordingSettings())

    val allRecordings: StateFlow<List<RecordingEntity>> = recordingRepository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRecordings: StateFlow<List<RecordingEntity>> = combine(
        allRecordings,
        _searchQuery,
        _selectedFilter
    ) { list, query, filter ->
        list.filter { item ->
            val matchesFilter = when (filter) {
                RecordingFilter.ALL -> true
                RecordingFilter.INCOMING -> item.callType == CallType.INCOMING.id
                RecordingFilter.OUTGOING -> item.callType == CallType.OUTGOING.id
                RecordingFilter.MANUAL -> item.callType == CallType.MANUAL.id
                RecordingFilter.FAVORITES -> item.isFavorite
            }
            val q = query.trim()
            val matchesQuery = q.isEmpty() ||
                item.title.contains(q, ignoreCase = true) ||
                item.fileName.contains(q, ignoreCase = true) ||
                item.formatId.contains(q, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveRecorderState: StateFlow<LiveRecorderState> = CallRecorderService.liveState
    val playbackState: StateFlow<PlaybackUiState> = playbackManager.state

    fun refreshDeviceStatusAndSyncService() {
        val status = DeviceCompatibilityManager.inspectDevice(app)
        _deviceStatus.value = status
        viewModelScope.launch {
            val currentSettings = settingsRepository.getCurrentSettings()
            if (status.allCorePermissionsGranted && currentSettings.autoRecordEnabled && currentSettings.keepMonitorServiceAlive) {
                CallRecorderService.startMonitoring(app)
            } else if (!currentSettings.autoRecordEnabled || !currentSettings.keepMonitorServiceAlive) {
                CallRecorderService.stopMonitoring(app)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: RecordingFilter) {
        _selectedFilter.value = filter
    }

    fun toggleAutoRecord() {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(autoRecordEnabled = !it.autoRecordEnabled)
            }
            refreshDeviceStatusAndSyncService()
        }
    }

    fun toggleManualRecording() {
        val status = DeviceCompatibilityManager.inspectDevice(app)
        _deviceStatus.value = status
        if (!status.hasRecordAudio) {
            Toast.makeText(app, "ابتدا دسترسی میکروفون را تایید کنید", Toast.LENGTH_SHORT).show()
            return
        }
        if (liveRecorderState.value.isRecording) {
            CallRecorderService.stopRecording(app)
        } else {
            CallRecorderService.startRecording(app, CallType.MANUAL)
        }
    }

    fun updateSettings(transform: (RecordingSettings) -> RecordingSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform)
            refreshDeviceStatusAndSyncService()
        }
    }

    fun setAudioFormat(format: AudioFormatOption) {
        updateSettings { it.copy(audioFormat = format) }
    }

    fun setAudioSource(source: AudioSourceOption) {
        updateSettings { it.copy(audioSource = source) }
    }

    fun togglePlay(recording: RecordingEntity) {
        playbackManager.togglePlay(recording)
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun cyclePlaybackSpeed() {
        playbackManager.cycleSpeed()
    }

    fun stopPlayback() {
        playbackManager.stop()
    }

    fun renameRecording(id: Long, newTitle: String) {
        viewModelScope.launch {
            recordingRepository.rename(id, newTitle)
        }
    }

    fun toggleFavorite(recording: RecordingEntity) {
        viewModelScope.launch {
            recordingRepository.toggleFavorite(recording.id, recording.isFavorite)
        }
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            if (playbackState.value.activeRecordingId == recording.id) {
                playbackManager.stop()
            }
            recordingRepository.delete(recording)
        }
    }

    fun deleteAllRecordings() {
        viewModelScope.launch {
            playbackManager.stop()
            recordingRepository.deleteAll()
        }
    }

    fun shareRecording(recording: RecordingEntity) {
        MediaStoreExporter.shareRecording(app, recording)
    }

    fun exportToMusic(recording: RecordingEntity) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) {
                MediaStoreExporter.exportToMusicFolder(
                    context = app,
                    sourceFile = File(recording.filePath),
                    displayTitle = recording.title,
                    mimeType = recording.mimeType
                )
            }
            val msg = if (uri != null) {
                "در پوشه Music/Cyberi ذخیره شد"
            } else {
                "فایل در حافظه داخلی برنامه موجود است"
            }
            Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        playbackManager.stop()
        super.onCleared()
    }
}
