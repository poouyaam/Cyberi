package com.example.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionItemStatus(
    val permission: String,
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val isEssential: Boolean
)

data class PermissionSummary(
    val hasRecordAudio: Boolean = false,
    val hasReadPhoneState: Boolean = false,
    val hasReadContacts: Boolean = false,
    val hasPostNotifications: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val items: List<PermissionItemStatus> = emptyList()
) {
    val allEssentialGranted: Boolean
        get() = hasRecordAudio && hasReadPhoneState

    val allFullAutomationGranted: Boolean
        get() = hasRecordAudio && hasReadPhoneState && hasReadContacts && hasPostNotifications

    val grantedCount: Int
        get() = items.count { it.isGranted }

    val totalCount: Int
        get() = items.size
}

class PermissionHandlingService(private val context: Context) {

    private val _permissionSummary = MutableStateFlow(evaluatePermissions())
    val permissionSummary: StateFlow<PermissionSummary> = _permissionSummary.asStateFlow()

    fun refresh(): PermissionSummary {
        val evaluated = evaluatePermissions()
        _permissionSummary.value = evaluated
        return evaluated
    }

    fun getAllRequiredPermissions(): Array<String> {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return perms.toTypedArray()
    }

    fun getMissingPermissions(): Array<String> {
        return getAllRequiredPermissions()
            .filter { !isGranted(it) }
            .toTypedArray()
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun evaluatePermissions(): PermissionSummary {
        val mic = isGranted(Manifest.permission.RECORD_AUDIO)
        val phone = isGranted(Manifest.permission.READ_PHONE_STATE)
        val contacts = isGranted(Manifest.permission.READ_CONTACTS)
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val batteryIgnored = pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true

        val items = buildList {
            add(
                PermissionItemStatus(
                    permission = Manifest.permission.RECORD_AUDIO,
                    title = "ضبط صدا و میکروفون (RECORD_AUDIO)",
                    description = "برای ضبط شفاف صدای شما و طرف مقابل با وضوح بالا",
                    isGranted = mic,
                    isEssential = true
                )
            )
            add(
                PermissionItemStatus(
                    permission = Manifest.permission.READ_PHONE_STATE,
                    title = "تشخیص وضعیت تماس (READ_PHONE_STATE)",
                    description = "تشخیص هوشمند زمان پاسخ به تماس، تماس خروجی و قطع تماس",
                    isGranted = phone,
                    isEssential = true
                )
            )
            add(
                PermissionItemStatus(
                    permission = Manifest.permission.READ_CONTACTS,
                    title = "دسترسی مخاطبین (READ_CONTACTS)",
                    description = "شناسایی و برچسب‌گذاری نام مخاطب روی فایل‌های مکالمه",
                    isGranted = contacts,
                    isEssential = false
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    PermissionItemStatus(
                        permission = Manifest.permission.POST_NOTIFICATIONS,
                        title = "نمایش اعلان وضعیت (POST_NOTIFICATIONS)",
                        description = "اعلان فعال بودن سرویس ضبط خودکار و ذخیره موفق مکالمه",
                        isGranted = notif,
                        isEssential = false
                    )
                )
            }
        }

        return PermissionSummary(
            hasRecordAudio = mic,
            hasReadPhoneState = phone,
            hasReadContacts = contacts,
            hasPostNotifications = notif,
            isBatteryOptimizationIgnored = batteryIgnored,
            items = items
        )
    }
}
