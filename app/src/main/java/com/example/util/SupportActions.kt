package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SupportActions {
    const val DEVELOPER_NAME = "پویا"
    const val SUPPORT_EMAIL = "info@amopouya.ir"
    const val SUPPORT_PHONE = "09011115366"

    fun openGmailSupport(context: Context) {
        val mailUri = Uri.parse("mailto:$SUPPORT_EMAIL")
        val gmailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
            setPackage("com.google.android.gm")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "پشتیبانی و راهنمایی برنامه سایبری")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val startedGmail = runCatching {
            context.startActivity(gmailIntent)
            true
        }.getOrDefault(false)

        if (!startedGmail) {
            val fallbackIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "پشتیبانی و راهنمایی برنامه سایبری")
            }
            val chooser = Intent.createChooser(fallbackIntent, "ارسال ایمیل پشتیبانی")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching {
                context.startActivity(chooser)
            }.onFailure {
                Toast.makeText(context, "برنامه ایمیل روی گوشی یافت نشد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openDialer(context: Context) {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SUPPORT_PHONE")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(dialIntent)
        }.onFailure {
            Toast.makeText(context, "باز کردن شماره‌گیر با خطا مواجه شد", Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun shareCyberiApp(context: Context) {
        val sharedApkUri = withContext(Dispatchers.IO) {
            runCatching {
                val sourceApk = File(context.applicationInfo.sourceDir)
                if (!sourceApk.exists() || !sourceApk.canRead()) return@runCatching null
                val destApk = File(context.cacheDir, "Cyberi_CallRecorder.apk")
                sourceApk.copyTo(destApk, overwrite = true)
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destApk
                )
            }.getOrNull()
        }

        if (sharedApkUri != null) {
            val shareApkIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, sharedApkUri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "برنامه ضبط هوشمند مکالمات تلفنی «سایبری»\nپشتیبانی: $SUPPORT_EMAIL"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareApkIntent, "اشتراک‌گذاری فایل نصبی برنامه سایبری")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(chooser) }
        } else {
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "برنامه ضبط تماس سایبری")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "برنامه ضبط هوشمند مکالمات تلفنی «سایبری» با کیفیت بالا و ذخیره خودکار\nطراحی و توسعه: $DEVELOPER_NAME\nپشتیبانی: $SUPPORT_EMAIL"
                )
            }
            val chooser = Intent.createChooser(textIntent, "اشتراک‌گذاری برنامه سایبری")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(chooser) }
        }
    }
}
