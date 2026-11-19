package com.hydrogen.bot.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.hydrogen.bot.MainActivity
import com.hydrogen.bot.R
import kotlin.math.abs

/**
 * FloatingJarvisService
 * Draggable futuristic glowing JARVIS HUD orb overlay.
 * Stays on screen across any app to trigger voice commands,
 * autonomous scrolling, and quick system navigation without switching apps.
 */
class FloatingJarvisService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingRoot: FrameLayout? = null
    private var orbView: FrameLayout? = null
    private var menuPanel: LinearLayout? = null
    private var isMenuExpanded = false

    private lateinit var layoutParams: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForegroundServiceNotification()
        createFloatingHud()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "jarvis_floating_channel"
        val channelName = "JARVIS Floating Controller"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Hydrogen JARVIS Active")
            .setContentText("Floating Phone Controller is active on screen.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingHud() {
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Overlay permission not granted.")
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 350
        }

        // Root container
        floatingRoot = FrameLayout(this)

        // Main Cyber Orb (56dp circle)
        val orbSize = dpToPx(56)
        orbView = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(orbSize, orbSize)

            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#121A2A")) // Dark Obsidian Slate
                setStroke(dpToPx(2), Color.parseColor("#00E5FF")) // Glowing Cyan
            }
            background = backgroundDrawable

            val micIcon = ImageView(this@FloatingJarvisService).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setColorFilter(Color.parseColor("#00E5FF"))
                val iconSize = dpToPx(28)
                val params = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
                layoutParams = params
            }
            addView(micIcon)
        }

        // Expanded Control Panel (Mini JARVIS Actions)
        menuPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            val panelBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                setColor(Color.parseColor("#182338"))
                setStroke(dpToPx(1), Color.parseColor("#00E5FF"))
            }
            background = panelBg
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

            // Action 1: 🎙️ Voice
            addView(createMiniButton("🎙️") {
                toggleMenu()
                launchVoicePrompt()
            })

            // Action 2: ⬇️ Scroll Down
            addView(createMiniButton("⬇️") {
                val ok = HydrogenAccessibilityService.performScrollDown()
                if (!ok) promptAccessibility()
            })

            // Action 3: ⬆️ Scroll Up
            addView(createMiniButton("⬆️") {
                val ok = HydrogenAccessibilityService.performScrollUp()
                if (!ok) promptAccessibility()
            })

            // Action 4: 🏠 Home
            addView(createMiniButton("🏠") {
                toggleMenu()
                HydrogenAccessibilityService.performHome()
            })

            // Action 5: ❌ Close Floating
            addView(createMiniButton("❌") {
                stopSelf()
            })
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(orbView)
            val menuParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
            }
            addView(menuPanel, menuParams)
        }

        floatingRoot?.addView(mainContainer)

        // Drag & Touch Logic
        setupTouchListener()

        try {
            windowManager?.addView(floatingRoot, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding floating window: ${e.message}")
        }
    }

    private fun createMiniButton(emoji: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = emoji
            textSize = 18f
            gravity = Gravity.CENTER
            val size = dpToPx(38)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(4)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun toggleMenu() {
        isMenuExpanded = !isMenuExpanded
        menuPanel?.visibility = if (isMenuExpanded) View.VISIBLE else View.GONE
    }

    private fun launchVoicePrompt() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_TRIGGER_VOICE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun promptAccessibility() {
        Toast.makeText(this, "JARVIS: Kripya Accessibility me Hydrogen Bot ON karein.", Toast.LENGTH_SHORT).show()
        HydrogenAccessibilityService.openAccessibilitySettings(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false

        orbView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isMoved = true
                        layoutParams.x = initialX + dx
                        layoutParams.y = initialY + dy
                        windowManager?.updateViewLayout(floatingRoot, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        // It was a tap!
                        toggleMenu()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        floatingRoot?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing floating view: ${e.message}")
            }
        }
        floatingRoot = null
    }

    companion object {
        private const val TAG = "FloatingJarvisService"
        const val ACTION_TRIGGER_VOICE = "com.hydrogen.bot.ACTION_TRIGGER_VOICE"

        @Volatile
        var isRunning: Boolean = false

        fun start(context: Context) {
            if (Settings.canDrawOverlays(context)) {
                val intent = Intent(context, FloatingJarvisService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingJarvisService::class.java))
        }
    }
}
