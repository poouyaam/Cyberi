package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.util.DeviceCompatibilityManager
import kotlin.math.abs

/**
 * Compact, draggable floating quick-record bubble that overlays the call screen
 * during incoming/outgoing calls (or whenever always-on mode is enabled).
 */
class FloatingCallButtonController(
    private val context: Context,
    private val onToggleInstantRecord: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val prefs = context.getSharedPreferences(PREFS_OVERLAY, Context.MODE_PRIVATE)

    private var containerView: LinearLayout? = null
    private var iconView: ImageView? = null
    private var labelView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isVisible = false

    fun canDraw(): Boolean = Settings.canDrawOverlays(context)

    @SuppressLint("ClickableViewAccessibility")
    fun show(isRecording: Boolean, elapsedMs: Long) {
        val wm = windowManager ?: return
        if (!canDraw()) return
        if (isVisible && containerView != null) {
            updateState(isRecording, elapsedMs)
            return
        }

        val density = context.resources.displayMetrics.density
        val padH = (12 * density).toInt()
        val padV = (9 * density).toInt()
        val iconSize = (20 * density).toInt()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(padH, padV, padH, padV)
            elevation = 10f * density
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                marginEnd = (6 * density).toInt()
            }
        }

        val label = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTextColor(Color.WHITE)
            isSingleLine = true
        }

        root.addView(icon)
        root.addView(label)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val savedX = prefs.getInt(KEY_POS_X, (16 * density).toInt())
        val savedY = prefs.getInt(KEY_POS_Y, (180 * density).toInt())

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        var startX = 0
        var startY = 0
        var touchStartX = 0f
        var touchStartY = 0f
        var isDragging = false

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    isDragging = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchStartX).toInt()
                    val dy = (event.rawY - touchStartY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isDragging = true
                    }
                    params.x = startX + dx
                    params.y = startY + dy
                    runCatching {
                        wm.updateViewLayout(root, params)
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        prefs.edit()
                            .putInt(KEY_POS_X, params.x)
                            .putInt(KEY_POS_Y, params.y)
                            .apply()
                    } else {
                        onToggleInstantRecord()
                    }
                    true
                }

                else -> false
            }
        }

        containerView = root
        iconView = icon
        labelView = label
        layoutParams = params

        updateState(isRecording, elapsedMs)

        runCatching {
            wm.addView(root, params)
            isVisible = true
        }.onFailure {
            containerView = null
            iconView = null
            labelView = null
            isVisible = false
        }
    }

    fun updateState(isRecording: Boolean, elapsedMs: Long) {
        val root = containerView ?: return
        val icon = iconView ?: return
        val label = labelView ?: return
        val density = context.resources.displayMetrics.density

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 26f * density
            if (isRecording) {
                setColor(Color.parseColor("#DC1B223C"))
                setStroke((1.6f * density).toInt(), Color.parseColor("#FF4D8D"))
            } else {
                setColor(Color.parseColor("#DC0B1528"))
                setStroke((1.6f * density).toInt(), Color.parseColor("#00E5D1"))
            }
        }
        root.background = bg

        if (isRecording) {
            icon.setImageResource(android.R.drawable.ic_media_pause)
            icon.setColorFilter(Color.parseColor("#FF4D8D"))
            label.text = "● ${DeviceCompatibilityManager.formatDuration(elapsedMs)}"
            label.setTextColor(Color.parseColor("#FFE4EC"))
        } else {
            icon.setImageResource(android.R.drawable.ic_btn_speak_now)
            icon.setColorFilter(Color.parseColor("#00F5D4"))
            label.text = "ضبط فوری"
            label.setTextColor(Color.WHITE)
        }
    }

    fun hide() {
        val root = containerView ?: return
        if (isVisible) {
            runCatching { windowManager?.removeViewImmediate(root) }
        }
        containerView = null
        iconView = null
        labelView = null
        layoutParams = null
        isVisible = false
    }

    companion object {
        private const val PREFS_OVERLAY = "cyberi_floating_overlay"
        private const val KEY_POS_X = "pos_x"
        private const val KEY_POS_Y = "pos_y"
    }
}
