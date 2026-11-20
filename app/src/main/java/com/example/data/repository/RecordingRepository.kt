package com.example.data.repository

import android.content.Context
import android.os.Build
import android.os.Environment
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

    suspend fun setContactName(id: Long, contactName: String?) = withContext(Dispatchers.IO) {
        recordingDao.updateContactName(id, contactName?.trim()?.takeIf { it.isNotEmpty() })
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

    suspend fun enforceStorageLimit(limitBytes: Long, autoDeleteOldest: Boolean): Int = withContext(Dispatchers.IO) {
        if (limitBytes <= 0L || !autoDeleteOldest) return@withContext 0
        var totalBytes = recordingDao.getTotalStorageBytes()
        if (totalBytes <= limitBytes) return@withContext 0

        var deletedCount = 0
        val oldestList = recordingDao.getAllOldestFirst()
        // Delete oldest non-favorite recordings first to stay within configured limit
        for (item in oldestList) {
            if (totalBytes <= limitBytes) break
            if (!item.isFavorite) {
                runCatching {
                    val f = File(item.filePath)
                    if (f.exists()) f.delete()
                }
                recordingDao.deleteById(item.id)
                totalBytes -= item.fileSizeBytes
                deletedCount++
            }
        }
        deletedCount
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        val dirType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Environment.DIRECTORY_RECORDINGS
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
