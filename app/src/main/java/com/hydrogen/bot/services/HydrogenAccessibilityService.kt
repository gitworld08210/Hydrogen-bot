package com.hydrogen.bot.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * HydrogenAccessibilityService
 * Autonomous Phone Controller inspired by JARVIS.
 * Provides autonomous hands-free UI clicking, screen scrolling,
 * WhatsApp automated message dispatch, and global device navigation.
 */
class HydrogenAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isSendingAttemptInProgress = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.i(TAG, "Hydrogen JARVIS Accessibility Service Connected.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkgName = event.packageName?.toString() ?: ""

        // WhatsApp Autonomous Message Dispatch Watcher with Multi-Attempt Retries
        if (armWhatsAppAutoSend && (pkgName == "com.whatsapp" || pkgName == "com.whatsapp.w4b")) {
            tryAutoSendWhatsAppWithRetries()
        }
    }

    private fun tryAutoSendWhatsAppWithRetries() {
        if (!armWhatsAppAutoSend || isSendingAttemptInProgress) return
        isSendingAttemptInProgress = true

        val delays = listOf(400L, 800L, 1200L, 1800L, 2500L, 3500L)
        fun scheduleNext(index: Int) {
            if (index >= delays.size || !armWhatsAppAutoSend) {
                isSendingAttemptInProgress = false
                return
            }
            mainHandler.postDelayed({
                val sent = autoSendWhatsApp()
                if (sent) {
                    Log.i(TAG, "Autonomous WhatsApp Send Click Successful on attempt ${index + 1}!")
                    armWhatsAppAutoSend = false
                    isSendingAttemptInProgress = false
                } else {
                    scheduleNext(index + 1)
                }
            }, delays[index])
        }
        scheduleNext(0)
    }

    override fun onInterrupt() {
        Log.w(TAG, "Hydrogen Accessibility Service Interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "Hydrogen Accessibility Service Destroyed.")
    }

    companion object {
        private const val TAG = "HydrogenAccessService"

        @Volatile
        var instance: HydrogenAccessibilityService? = null

        @Volatile
        var armWhatsAppAutoSend: Boolean = false

        /**
         * Checks if the Accessibility Service is active and granted by the user.
         */
        fun isServiceEnabled(): Boolean = instance != null

        /**
         * Arms the WhatsApp auto-sender so the next time WhatsApp opens,
         * the service automatically taps the Send button.
         */
        fun armAutoSend(enable: Boolean = true) {
            armWhatsAppAutoSend = enable
        }

        /**
         * Opens the device Accessibility Settings page directly so the user can enable Hydrogen Bot.
         */
        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open accessibility settings: ${e.message}")
            }
        }

        /**
         * Autonomous Scroll Down / Next Video Swipe (Shorts, Reels, TikTok, Web)
         */
        fun performScrollDown(): Boolean {
            val s = instance ?: return false
            return s.dispatchScrollDown()
        }

        /**
         * Autonomous Scroll Up / Previous Video Swipe
         */
        fun performScrollUp(): Boolean {
            val s = instance ?: return false
            return s.dispatchScrollUp()
        }

        /**
         * Global Navigation Actions
         */
        fun performHome(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) ?: false
        fun performBack(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) ?: false
        fun performRecents(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_RECENTS) ?: false
        fun performNotifications(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ?: false

        /**
         * Autonomous Click on specific text on screen
         */
        fun clickByText(text: String): Boolean = instance?.findAndClickText(text) ?: false

        /**
         * Auto-clicks the WhatsApp Send button
         */
        fun autoSendWhatsApp(): Boolean = instance?.autoSendWhatsApp() ?: false
    }

    /**
     * Swipes UP to scroll down / load next video in Shorts/Reels/Feed
     */
    private fun dispatchScrollDown(): Boolean {
        return try {
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()

            val startX = width * 0.5f
            val startY = height * 0.78f
            val endY = height * 0.22f

            val swipePath = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }

            val stroke = GestureDescription.StrokeDescription(swipePath, 0, 240)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "dispatchScrollDown error: ${e.message}")
            // Fallback to node scroll action
            rootInActiveWindow?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
        }
    }

    /**
     * Swipes DOWN to scroll up / load previous video
     */
    private fun dispatchScrollUp(): Boolean {
        return try {
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.toFloat()
            val height = metrics.heightPixels.toFloat()

            val startX = width * 0.5f
            val startY = height * 0.25f
            val endY = height * 0.80f

            val swipePath = Path().apply {
                moveTo(startX, startY)
                lineTo(startX, endY)
            }

            val stroke = GestureDescription.StrokeDescription(swipePath, 0, 240)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "dispatchScrollUp error: ${e.message}")
            rootInActiveWindow?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) ?: false
        }
    }

    /**
     * Autonomous clicker for WhatsApp Send Button
     */
    fun autoSendWhatsApp(): Boolean {
        val root = rootInActiveWindow ?: return false

        // 1. WhatsApp native Send button view IDs (standard & WhatsApp Business)
        val candidateIds = listOf("com.whatsapp:id/send", "com.whatsapp:id/send_container", "com.whatsapp.w4b:id/send")
        for (id in candidateIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            for (node in nodes) {
                if (performClickOnNode(node)) return true
            }
        }

        // 2. Scan all nodes for "Send" or Hindi "भेजें"
        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        collectAllNodes(root, allNodes)

        for (node in allNodes) {
            val desc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""
            val text = node.text?.toString()?.trim()?.lowercase() ?: ""

            if (desc == "send" || desc == "भेजें" || text == "send" || text == "भेजें" ||
                desc.contains("send message") || desc.contains("संदेश भेजें")) {
                if (performClickOnNode(node)) return true
            }
        }
        return false
    }

    /**
     * Finds any clickable node with matching text or description and clicks it.
     */
    fun findAndClickText(target: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val cleanTarget = target.trim().lowercase()

        val allNodes = mutableListOf<AccessibilityNodeInfo>()
        collectAllNodes(root, allNodes)

        for (node in allNodes) {
            val text = node.text?.toString()?.trim()?.lowercase() ?: ""
            val desc = node.contentDescription?.toString()?.trim()?.lowercase() ?: ""

            if (text.contains(cleanTarget) || desc.contains(cleanTarget)) {
                if (performClickOnNode(node)) return true
            }
        }
        return false
    }

    private fun performClickOnNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (clicked) return true
        }
        // Check parents if wrapper holds click listener
        var parent = node.parent
        var depth = 0
        while (parent != null && depth < 3) {
            if (parent.isClickable) {
                val clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) return true
            }
            parent = parent.parent
            depth++
        }

        // Fallback: Dispatch exact coordinate gesture tap on the node
        try {
            val rect = android.graphics.Rect()
            node.getBoundsInScreen(rect)
            if (rect.width() > 0 && rect.height() > 0) {
                val clickPath = Path().apply {
                    moveTo(rect.centerX().toFloat(), rect.centerY().toFloat())
                }
                val stroke = GestureDescription.StrokeDescription(clickPath, 0, 80)
                val gesture = GestureDescription.Builder().addStroke(stroke).build()
                dispatchGesture(gesture, null, null)
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gesture tap click error: ${e.message}")
        }

        return false
    }

    private fun collectAllNodes(parent: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (parent == null) return
        list.add(parent)
        for (i in 0 until parent.childCount) {
            collectAllNodes(parent.getChild(i), list)
        }
    }
}
