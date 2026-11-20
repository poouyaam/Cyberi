package com.example.service

import android.content.Context
import android.telephony.TelephonyManager
import com.example.model.CallType

sealed class CallTransition {
    data object None : CallTransition()
    data class StartRecording(val callType: CallType) : CallTransition()
    data object StopRecording : CallTransition()
}

object CallStateTracker {
    private const val PREFS_NAME = "cyberi_call_state"
    private const val KEY_LAST_STATE = "last_state"
    private const val KEY_WAS_RINGING = "was_ringing"
    private const val KEY_IN_ACTIVE_CALL = "in_active_call"

    @Synchronized
    fun onTelephonyStateChanged(context: Context, telephonyState: Int): CallTransition {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastState = prefs.getInt(KEY_LAST_STATE, TelephonyManager.CALL_STATE_IDLE)
        val wasRinging = prefs.getBoolean(KEY_WAS_RINGING, false)
        val inActiveCall = prefs.getBoolean(KEY_IN_ACTIVE_CALL, false)

        if (telephonyState == lastState) {
            return CallTransition.None
        }

        return when (telephonyState) {
            TelephonyManager.CALL_STATE_RINGING -> {
                prefs.edit()
                    .putInt(KEY_LAST_STATE, telephonyState)
                    .putBoolean(KEY_WAS_RINGING, true)
                    .apply()
                CallTransition.None
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                val callType = if (wasRinging || lastState == TelephonyManager.CALL_STATE_RINGING) {
                    CallType.INCOMING
                } else {
                    CallType.OUTGOING
                }
                prefs.edit()
                    .putInt(KEY_LAST_STATE, telephonyState)
                    .putBoolean(KEY_WAS_RINGING, false)
                    .putBoolean(KEY_IN_ACTIVE_CALL, true)
                    .apply()

                if (!inActiveCall) {
                    CallTransition.StartRecording(callType)
                } else {
                    CallTransition.None
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                prefs.edit()
                    .putInt(KEY_LAST_STATE, telephonyState)
                    .putBoolean(KEY_WAS_RINGING, false)
                    .putBoolean(KEY_IN_ACTIVE_CALL, false)
                    .apply()

                if (inActiveCall) {
                    CallTransition.StopRecording
                } else {
                    CallTransition.None
                }
            }

            else -> CallTransition.None
        }
    }

    @Synchronized
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_STATE, TelephonyManager.CALL_STATE_IDLE)
            .putBoolean(KEY_WAS_RINGING, false)
            .putBoolean(KEY_IN_ACTIVE_CALL, false)
            .apply()
    }
}
