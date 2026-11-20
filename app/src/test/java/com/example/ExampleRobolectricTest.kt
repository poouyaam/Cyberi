package com.example

import android.Manifest
import android.content.Context
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.example.model.AudioFormatOption
import com.example.model.CallType
import com.example.model.StorageLimitOption
import com.example.service.CallStateTracker
import com.example.service.CallTransition
import com.example.service.PermissionHandlingService
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
    fun `permission handling service includes mic phone state and contacts`() {
        val permService = PermissionHandlingService(context)
        val required = permService.getAllRequiredPermissions().toSet()
        assertTrue(required.contains(Manifest.permission.RECORD_AUDIO))
        assertTrue(required.contains(Manifest.permission.READ_PHONE_STATE))
        assertTrue(required.contains(Manifest.permission.READ_CONTACTS))
    }

    @Test
    fun `primary default audio format and storage limits resolve cleanly`() {
        val primary = AudioFormatOption.entries.single { it.isPrimaryDefault }
        assertEquals(AudioFormatOption.M4A_AAC, primary)
        assertEquals(7, AudioFormatOption.entries.size)
        AudioFormatOption.entries.forEach { format ->
            assertEquals(format, AudioFormatOption.fromId(format.id))
        }
        StorageLimitOption.entries.forEach { limit ->
            assertEquals(limit, StorageLimitOption.fromId(limit.id))
        }
    }
}
