package com.example

import android.app.Application
import com.example.data.db.CyberiDatabase
import com.example.data.repository.RecordingRepository
import com.example.data.repository.SettingsRepository

class CyberiApplication : Application() {
    val database: CyberiDatabase by lazy { CyberiDatabase.getInstance(this) }
    val recordingRepository: RecordingRepository by lazy {
        RecordingRepository(database.recordingDao(), this)
    }
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }
}
