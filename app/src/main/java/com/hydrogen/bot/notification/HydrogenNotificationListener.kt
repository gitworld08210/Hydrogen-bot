package com.hydrogen.bot.notification

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

data class IncomingChat(
    val sender: String,
    val message: String,
    val packageName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val replyAction: Notification.Action? = null,
    val remoteInput: RemoteInput? = null
)

/**
 * HydrogenNotificationListener:
 * Captures incoming messages from WhatsApp, Instagram, and Telegram.
 * Holds recent message context for the AI representative to draft contextual replies,
 * and allows direct background auto-replying via Android's native RemoteInput API.
 */
class HydrogenNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.i(TAG, "Hydrogen Notification Listener Connected.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (instance == this) {
            instance = null
        }
        Log.i(TAG, "Hydrogen Notification Listener Disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: ""
        // Filter messaging apps: WhatsApp, Instagram, Telegram, SMS
        if (pkg != "com.whatsapp" && pkg != "com.instagram.android" &&
            pkg != "org.telegram.messenger" && !pkg.contains("messaging") && !pkg.contains("mms")) {
            return
        }

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val finalMessage = bigText.ifBlank { text }.trim()

        // Skip blank messages or system/group status notifications
        if (title.isBlank() || finalMessage.isBlank() ||
            finalMessage.contains("messages from") || finalMessage.contains("new messages") ||
            finalMessage == "Checking for new messages…") {
            return
        }

        // Search for Direct Reply Action (RemoteInput)
        var replyAction: Notification.Action? = null
        var targetRemoteInput: RemoteInput? = null

        notification.actions?.forEach { action ->
            action.remoteInputs?.forEach { remoteInput ->
                if (remoteInput.resultKey.isNotBlank()) {
                    replyAction = action
                    targetRemoteInput = remoteInput
                }
            }
        }

        val chat = IncomingChat(
            sender = title,
            message = finalMessage,
            packageName = pkg,
            replyAction = replyAction,
            remoteInput = targetRemoteInput
        )

        lastIncomingChat = chat
        onIncomingChatReceived?.invoke(chat)
        Log.i(TAG, "Captured incoming chat from $title in $pkg: '$finalMessage'")
    }

    companion object {
        private const val TAG = "HydrogenNotifyListener"

        @Volatile
        var instance: HydrogenNotificationListener? = null

        @Volatile
        var lastIncomingChat: IncomingChat? = null

        var onIncomingChatReceived: ((IncomingChat) -> Unit)? = null

        /**
         * Checks if the Notification Listener service is active.
         */
        fun isServiceConnected(): Boolean = instance != null

        /**
         * Checks if Notification Listener permission is granted by the system.
         */
        fun isPermissionGranted(context: Context): Boolean {
            val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
            return listeners.contains(context.packageName)
        }

        /**
         * Opens Android Notification Access Settings for the user.
         */
        fun openNotificationAccessSettings(context: Context) {
            try {
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification settings: ${e.message}")
            }
        }

        /**
         * Dispatches a direct background reply to the last incoming message
         * using the native Android RemoteInput API.
         */
        fun sendDirectReply(context: Context, replyText: String): Boolean {
            val chat = lastIncomingChat ?: return false
            val action = chat.replyAction ?: return false
            val remoteInput = chat.remoteInput ?: return false

            return try {
                val replyIntent = Intent()
                val bundle = Bundle()
                bundle.putCharSequence(remoteInput.resultKey, replyText)
                RemoteInput.addResultsToIntent(arrayOf(remoteInput), replyIntent, bundle)

                action.actionIntent.send(context, 0, replyIntent)
                Log.i(TAG, "Direct RemoteInput reply sent to ${chat.sender}: '$replyText'")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send direct reply: ${e.message}")
                false
            }
        }
    }
}
