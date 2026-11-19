package com.hydrogen.bot.router

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

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
     * Launches Amazon Native App directly into the search results screen.
     * Guaranteed: Opens official native app if installed, NEVER opens browser!
     */
    fun launchAmazonSearch(context: Context, query: String): LaunchResult {
        val amazonPkg = getInstalledAmazonPackage(context)
        val encodedQuery = Uri.encode(query.trim())

        return if (amazonPkg != null) {
            try {
                // Targeted Native Intent - Locked to Amazon App package
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://www.amazon.in/s?k=$encodedQuery")
                    setPackage(amazonPkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "Amazon India",
                    isNativeApp = true,
                    message = "Amazon App me '$query' search khol diya hai."
                )
            } catch (e: Exception) {
                // Secondary In-App Scheme Attempt
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
                        message = "Amazon App me '$query' search khol diya hai."
                    )
                } catch (ex: Exception) {
                    LaunchResult(
                        success = false,
                        targetApp = "Amazon India",
                        isNativeApp = false,
                        message = "Amazon App launch karne me error: ${ex.localizedMessage}"
                    )
                }
            }
        } else {
            // App is not installed on device
            LaunchResult(
                success = false,
                targetApp = "Amazon India",
                isNativeApp = false,
                message = "Phone me Amazon app install nahi mili."
            )
        }
    }

    /**
     * Launches YouTube Native App directly with query.
     */
    fun launchYouTubeSearch(context: Context, query: String): LaunchResult {
        val isInstalled = isAppInstalled(context, PackageRegistry.YOUTUBE_PACKAGE)
        val encodedQuery = Uri.encode(query.trim())

        return if (isInstalled) {
            try {
                val intent = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(PackageRegistry.YOUTUBE_PACKAGE)
                    putExtra("query", query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "YouTube",
                    isNativeApp = true,
                    message = "YouTube App me '$query' play/search kar diya hai."
                )
            } catch (e: Exception) {
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
                    message = "YouTube App me '$query' khol diya hai."
                )
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
     * Launches Flipkart Native App directly with query.
     */
    fun launchFlipkartSearch(context: Context, query: String): LaunchResult {
        val isInstalled = isAppInstalled(context, PackageRegistry.FLIPKART_PACKAGE)
        val encodedQuery = Uri.encode(query.trim())

        return if (isInstalled) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("flipkart://search?q=$encodedQuery")
                    setPackage(PackageRegistry.FLIPKART_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "Flipkart",
                    isNativeApp = true,
                    message = "Flipkart App me '$query' search khol diya hai."
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
     * Launches WhatsApp chat with pre-filled message.
     */
    fun launchWhatsApp(context: Context, message: String): LaunchResult {
        val isInstalled = isAppInstalled(context, PackageRegistry.WHATSAPP_PACKAGE)
        return if (isInstalled) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage(PackageRegistry.WHATSAPP_PACKAGE)
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "WhatsApp",
                    isNativeApp = true,
                    message = "WhatsApp open ho gaya hai."
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
                message = "Phone me WhatsApp install nahi hai."
            )
        }
    }
}
