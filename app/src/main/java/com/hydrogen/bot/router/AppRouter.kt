package com.hydrogen.bot.router

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock

data class LaunchResult(
    val success: Boolean,
    val targetApp: String,
    val isNativeApp: Boolean,
    val message: String
)

object AppRouter {

    /**
     * Checks whether an Android app is installed using the PackageManager.
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Finds installed Amazon package (India or Global).
     */
    fun getInstalledAmazonPackage(context: Context): String? {
        return when {
            isAppInstalled(context, PackageRegistry.AMAZON_IN_PACKAGE) -> PackageRegistry.AMAZON_IN_PACKAGE
            isAppInstalled(context, PackageRegistry.AMAZON_GLOBAL_PACKAGE) -> PackageRegistry.AMAZON_GLOBAL_PACKAGE
            else -> null
        }
    }

    /**
     * Launches Amazon Native App directly into the search results screen or home screen.
     * Guaranteed: Opens official native app if installed, NEVER opens browser!
     */
    fun launchAmazonSearch(context: Context, query: String): LaunchResult {
        val amazonPkg = getInstalledAmazonPackage(context)
        val cleanQuery = query.trim()
        val encodedQuery = Uri.encode(cleanQuery)

        return if (amazonPkg != null) {
            try {
                val intent = if (cleanQuery.isBlank() || cleanQuery == "products") {
                    context.packageManager.getLaunchIntentForPackage(amazonPkg)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.amazon.in")).apply {
                        setPackage(amazonPkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://www.amazon.in/s?k=$encodedQuery")
                        setPackage(amazonPkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "Amazon India",
                    isNativeApp = true,
                    message = if (cleanQuery.isBlank()) "Amazon App open kar diya hai." else "Amazon App me '$cleanQuery' search khol diya hai."
                )
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("amazon://search?k=$encodedQuery")
                        setPackage(amazonPkg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    LaunchResult(
                        success = true,
                        targetApp = "Amazon India",
                        isNativeApp = true,
                        message = "Amazon App khol diya hai."
                    )
                } catch (ex: Exception) {
                    LaunchResult(
                        success = false,
                        targetApp = "Amazon India",
                        isNativeApp = false,
                        message = "Amazon App launch error: ${ex.localizedMessage}"
                    )
                }
            }
        } else {
            LaunchResult(
                success = false,
                targetApp = "Amazon India",
                isNativeApp = false,
                message = "Phone me Amazon app install nahi mili."
            )
        }
    }

    /**
     * Launches YouTube Native App directly with query or to home feed.
     */
    fun launchYouTubeSearch(context: Context, query: String): LaunchResult {
        val isInstalled = isAppInstalled(context, PackageRegistry.YOUTUBE_PACKAGE)
        val cleanQuery = query.trim()
        val encodedQuery = Uri.encode(cleanQuery)

        return if (isInstalled) {
            try {
                val intent = if (cleanQuery.isBlank() || cleanQuery == "trending songs") {
                    context.packageManager.getLaunchIntentForPackage(PackageRegistry.YOUTUBE_PACKAGE)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                        setPackage(PackageRegistry.YOUTUBE_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(Intent.ACTION_SEARCH).apply {
                        setPackage(PackageRegistry.YOUTUBE_PACKAGE)
                        putExtra("query", cleanQuery)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "YouTube",
                    isNativeApp = true,
                    message = if (cleanQuery.isBlank()) "YouTube App open kar diya hai." else "YouTube App me '$cleanQuery' khol diya hai."
                )
            } catch (e: Exception) {
                try {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("vnd.youtube:results?search_query=$encodedQuery")
                        setPackage(PackageRegistry.YOUTUBE_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(viewIntent)
                    LaunchResult(
                        success = true,
                        targetApp = "YouTube",
                        isNativeApp = true,
                        message = "YouTube App khol diya hai."
                    )
                } catch (ex: Exception) {
                    LaunchResult(
                        success = false,
                        targetApp = "YouTube",
                        isNativeApp = false,
                        message = "YouTube launch error: ${ex.localizedMessage}"
                    )
                }
            }
        } else {
            LaunchResult(
                success = false,
                targetApp = "YouTube",
                isNativeApp = false,
                message = "Phone me YouTube app install nahi mili."
            )
        }
    }

    /**
     * Launches Flipkart Native App directly with query or to home feed.
     */
    fun launchFlipkartSearch(context: Context, query: String): LaunchResult {
        val isInstalled = isAppInstalled(context, PackageRegistry.FLIPKART_PACKAGE)
        val cleanQuery = query.trim()
        val encodedQuery = Uri.encode(cleanQuery)

        return if (isInstalled) {
            try {
                val intent = if (cleanQuery.isBlank() || cleanQuery == "deals") {
                    context.packageManager.getLaunchIntentForPackage(PackageRegistry.FLIPKART_PACKAGE)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("flipkart://home")).apply {
                        setPackage(PackageRegistry.FLIPKART_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("flipkart://search?q=$encodedQuery")
                        setPackage(PackageRegistry.FLIPKART_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "Flipkart",
                    isNativeApp = true,
                    message = if (cleanQuery.isBlank()) "Flipkart App open kar diya hai." else "Flipkart App me '$cleanQuery' search khol diya hai."
                )
            } catch (e: Exception) {
                LaunchResult(
                    success = false,
                    targetApp = "Flipkart",
                    isNativeApp = false,
                    message = "Flipkart launch error: ${e.localizedMessage}"
                )
            }
        } else {
            LaunchResult(
                success = false,
                targetApp = "Flipkart",
                isNativeApp = false,
                message = "Phone me Flipkart app install nahi mili."
            )
        }
    }

    /**
     * Launches WhatsApp directly with optional pre-filled message text.
     */
    fun launchWhatsApp(context: Context, query: String): LaunchResult {
        val isInstalled = isAppInstalled(context, PackageRegistry.WHATSAPP_PACKAGE)
        val cleanMsg = query.trim()

        return if (isInstalled) {
            try {
                val intent = if (cleanMsg.isNotBlank()) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        setPackage(PackageRegistry.WHATSAPP_PACKAGE)
                        putExtra(Intent.EXTRA_TEXT, cleanMsg)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    context.packageManager.getLaunchIntentForPackage(PackageRegistry.WHATSAPP_PACKAGE)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    } ?: Intent(Intent.ACTION_MAIN).apply {
                        setPackage(PackageRegistry.WHATSAPP_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "WhatsApp",
                    isNativeApp = true,
                    message = if (cleanMsg.isNotBlank()) "WhatsApp me message draft kar diya hai." else "WhatsApp open kar diya hai."
                )
            } catch (e: Exception) {
                LaunchResult(
                    success = false,
                    targetApp = "WhatsApp",
                    isNativeApp = false,
                    message = "WhatsApp launch error: ${e.localizedMessage}"
                )
            }
        } else {
            LaunchResult(
                success = false,
                targetApp = "WhatsApp",
                isNativeApp = false,
                message = "Phone me WhatsApp app install nahi mili."
            )
        }
    }

    /**
     * Launches Blinkit Native App (com.grofers.customerapp) directly into search or home.
     */
    fun launchBlinkitSearch(context: Context, query: String): LaunchResult {
        val isInstalled = isAppInstalled(context, PackageRegistry.BLINKIT_PACKAGE)
        val cleanQuery = query.trim()
        val encodedQuery = Uri.encode(cleanQuery)

        return if (isInstalled) {
            try {
                val intent = if (cleanQuery.isBlank()) {
                    context.packageManager.getLaunchIntentForPackage(PackageRegistry.BLINKIT_PACKAGE)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    } ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://blinkit.com")).apply {
                        setPackage(PackageRegistry.BLINKIT_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://blinkit.com/s/?q=$encodedQuery")
                        setPackage(PackageRegistry.BLINKIT_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "Blinkit",
                    isNativeApp = true,
                    message = if (cleanQuery.isBlank()) "Blinkit App open kar diya hai." else "Blinkit App par '$cleanQuery' search khol diya hai."
                )
            } catch (e: Exception) {
                try {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("grofers://search?q=$encodedQuery")
                        setPackage(PackageRegistry.BLINKIT_PACKAGE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                    LaunchResult(
                        success = true,
                        targetApp = "Blinkit",
                        isNativeApp = true,
                        message = "Blinkit App khol diya hai."
                    )
                } catch (ex: Exception) {
                    LaunchResult(
                        success = false,
                        targetApp = "Blinkit",
                        isNativeApp = false,
                        message = "Blinkit launch error: ${ex.localizedMessage}"
                    )
                }
            }
        } else {
            // Web / browser fallback
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://blinkit.com/s/?q=$encodedQuery")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                LaunchResult(
                    success = true,
                    targetApp = "Blinkit Web",
                    isNativeApp = false,
                    message = "Blinkit app phone me nahi mili, browser me search open kar diya hai."
                )
            } catch (e: Exception) {
                LaunchResult(
                    success = false,
                    targetApp = "Blinkit",
                    isNativeApp = false,
                    message = "Phone me Blinkit app install nahi mili."
                )
            }
        }
    }

    /**
     * Sets a native system alarm using Android AlarmClock API.
     */
    fun launchSetAlarm(context: Context, query: String): LaunchResult {
        var hour = 5
        var minute = 0
        val clean = query.lowercase()

        val isPm = clean.contains("pm") || clean.contains("shaam") || clean.contains("sham") ||
                   clean.contains("raat") || clean.contains("dopahar") || clean.contains("शाम") || clean.contains("रात")

        val isAm = clean.contains("am") || clean.contains("subah") || clean.contains("prabhat") || clean.contains("सुबह")

        // 1. Check for HH:MM (e.g. 5:30, 06:15)
        val colonMatch = Regex("(\\d{1,2}):(\\d{2})").find(clean)
        if (colonMatch != null) {
            hour = colonMatch.groupValues[1].toIntOrNull() ?: 5
            minute = colonMatch.groupValues[2].toIntOrNull() ?: 0
        } else {
            // 2. Check for single numbers (e.g. 5, 6, 7)
            val numberMatch = Regex("(\\d{1,2})").find(clean)
            if (numberMatch != null) {
                hour = numberMatch.groupValues[1].toIntOrNull() ?: 5
                minute = 0
            }
        }

        if (isPm && hour < 12) {
            hour += 12
        } else if (isAm && hour == 12) {
            hour = 0
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Hydrogen Alarm")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val formattedMin = if (minute < 10) "0$minute" else "$minute"
            val amPmStr = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            LaunchResult(
                success = true,
                targetApp = "Clock",
                isNativeApp = true,
                message = "$displayHour:$formattedMin $amPmStr ka alarm set kar diya hai."
            )
        } catch (e: Exception) {
            LaunchResult(
                success = false,
                targetApp = "Clock",
                isNativeApp = false,
                message = "Alarm set karne me error: ${e.message}"
            )
        }
    }

    /**
     * Sets a native system countdown timer using Android AlarmClock API.
     */
    fun launchSetTimer(context: Context, query: String): LaunchResult {
        var seconds = 60
        val clean = query.lowercase()

        val minuteMatch = Regex("(\\d+)\\s*(?:min|minute|minut|मिनट)").find(clean)
        val secondMatch = Regex("(\\d+)\\s*(?:sec|second|sekand|सेकंड)").find(clean)

        if (minuteMatch != null) {
            val mins = minuteMatch.groupValues[1].toIntOrNull() ?: 1
            seconds = mins * 60
        } else if (secondMatch != null) {
            seconds = secondMatch.groupValues[1].toIntOrNull() ?: 30
        } else {
            val anyNumber = Regex("(\\d+)").find(clean)
            if (anyNumber != null) {
                val num = anyNumber.groupValues[1].toIntOrNull() ?: 1
                seconds = num * 60
            }
        }

        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, "Hydrogen Timer")
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            LaunchResult(
                success = true,
                targetApp = "Clock",
                isNativeApp = true,
                message = "${seconds / 60} minute ka timer set kar diya hai."
            )
        } catch (e: Exception) {
            LaunchResult(
                success = false,
                targetApp = "Clock",
                isNativeApp = false,
                message = "Timer set karne me error: ${e.message}"
            )
        }
    }
}
