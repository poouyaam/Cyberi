package com.example

import android.content.Context
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioFormatOption
import com.example.model.CallType
import com.example.service.CallStateTracker
import com.example.service.CallTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CallStateTracker.reset(context)
    }

    @Test
    fun `app name is Cyberi in Persian`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("سایبری", appName)
    }

    @Test
    fun `incoming call answered starts incoming recording and idle stops it`() {
        val ringing = CallStateTracker.onTelephonyStateChanged(context, TelephonyManager.CALL_STATE_RINGING)
        assertEquals(CallTransition.None, ringing)

        val answered = CallStateTracker.onTelephonyStateChanged(context, TelephonyManager.CALL_STATE_OFFHOOK)
        assertEquals(CallTransition.StartRecording(CallType.INCOMING), answered)

        // Duplicate OFFHOOK broadcast must not trigger again
        val duplicateOffhook = CallStateTracker.onTelephonyStateChanged(context, TelephonyManager.CALL_STATE_OFFHOOK)
        assertEquals(CallTransition.None, duplicateOffhook)

        val ended = CallStateTracker.onTelephonyStateChanged(context, TelephonyManager.CALL_STATE_IDLE)
        assertEquals(CallTransition.StopRecording, ended)
    }

    @Test
    fun `outgoing call starts outgoing recording immediately and stops on idle`() {
        val dialing = CallStateTracker.onTelephonyStateChanged(context, TelephonyManager.CALL_STATE_OFFHOOK)
        assertEquals(CallTransition.StartRecording(CallType.OUTGOING), dialing)

        val hungUp = CallStateTracker.onTelephonyStateChanged(context, TelephonyManager.CALL_STATE_IDLE)
        assertEquals(CallTransition.StopRecording, hungUp)
    }

    @Test
    fun `missed incoming call does not start or stop recording`() {
        assertEquals(
            CallTransition.None,
            CallStateTracker.onTelephonyStateChanged(context, TelephonyManager.CALL_STATE_RINGING)
        )
        assertEquals(
            CallTransition.None,
            CallStateTracker.onTelephonyStateChanged(context, TelephonyManager.CALL_STATE_IDLE)
        )
    }

    @Test
    fun `primary default audio format is M4A_AAC and all formats resolve`() {
        val primary = AudioFormatOption.entries.single { it.isPrimaryDefault }
        assertEquals(AudioFormatOption.M4A_AAC, primary)
        assertTrue(AudioFormatOption.entries.size == 7)
        AudioFormatOption.entries.forEach { format ->
            assertEquals(format, AudioFormatOption.fromId(format.id))
        }
    }
}
