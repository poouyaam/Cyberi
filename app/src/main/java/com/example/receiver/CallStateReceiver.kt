package com.example.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.data.repository.SettingsRepository
import com.example.model.CallType
import com.example.service.CallRecorderService
import com.example.service.CallStateTracker
import com.example.service.CallTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMic) return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                if (android.os.Build.VERSION.SDK_INT < 35) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            CallStateTracker.reset(context)
                            val settings = SettingsRepository(context).getCurrentSettings()
                            if (settings.autoRecordEnabled && settings.keepMonitorServiceAlive) {
                                CallRecorderService.startMonitoring(context)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                @Suppress("DEPRECATION")
                val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
                val stateInt = when (stateStr) {
                    TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
                    TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
                    else -> return
                }

                when (stateInt) {
                    TelephonyManager.CALL_STATE_RINGING,
                    TelephonyManager.CALL_STATE_OFFHOOK -> CallRecorderService.showCallOverlay(context)
                    TelephonyManager.CALL_STATE_IDLE -> CallRecorderService.hideCallOverlay(context)
                }

                val transition = CallStateTracker.onTelephonyStateChanged(context, stateInt)
                when (transition) {
                    is CallTransition.StartRecording -> {
                        val pendingResult = goAsync()
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val settings = SettingsRepository(context).getCurrentSettings()
                                if (!settings.autoRecordEnabled) return@launch
                                val shouldRecord = when (transition.callType) {
                                    CallType.INCOMING -> settings.recordIncomingCalls
                                    CallType.OUTGOING -> settings.recordOutgoingCalls
                                    CallType.MANUAL -> true
                                }
                                if (shouldRecord) {
                                    CallRecorderService.startRecording(context, transition.callType)
                                }
                            } finally {
                                pendingResult.finish()
                            }
                        }
                    }

                    is CallTransition.StopRecording -> {
                        CallRecorderService.stopRecording(context)
                    }

                    CallTransition.None -> Unit
                }
            }
        }
    }
}
