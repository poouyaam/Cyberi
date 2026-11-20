package com.example.data.repository

import android.content.Context
import com.example.data.db.RecordingDao
import com.example.data.db.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(
    private val recordingDao: RecordingDao,
    private val appContext: Context
) {
    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    suspend fun insert(recording: RecordingEntity): Long = withContext(Dispatchers.IO) {
        recordingDao.insertRecording(recording)
    }

    suspend fun rename(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isNotEmpty()) {
            recordingDao.renameRecording(id, cleanTitle)
        }
    }

    suspend fun toggleFavorite(id: Long, current: Boolean) = withContext(Dispatchers.IO) {
        recordingDao.setFavorite(id, !current)
    }

    suspend fun delete(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        }
        recordingDao.deleteById(recording.id)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val dirType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.os.Environment.DIRECTORY_RECORDINGS
        } else {
            "Recordings"
        }
        val dir = appContext.getExternalFilesDir(dirType)
            ?: File(appContext.filesDir, "recordings")
        runCatching {
            dir.listFiles()?.forEach { it.delete() }
        }
        recordingDao.deleteAll()
    }
}
