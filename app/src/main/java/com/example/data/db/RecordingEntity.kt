package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val callType: String,
    val filePath: String,
    val fileName: String,
    val formatId: String,
    val mimeType: String,
    val sampleRateHz: Int,
    val bitrateKbps: Int,
    val audioSourceUsed: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val timestamp: Long,
    val isFavorite: Boolean = false,
    val exportedUri: String? = null,
    val waveformSamples: String = ""
)
