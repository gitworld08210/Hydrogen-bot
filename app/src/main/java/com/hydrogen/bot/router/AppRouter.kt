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
     * Launches WhatsApp directly.
     */
    fun launchWhatsApp(context: Context, query: String): LaunchResult {
        val isInstalled = isAppInstalled(context, PackageRegistry.WHATSAPP_PACKAGE)

        return if (isInstalled) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(PackageRegistry.WHATSAPP_PACKAGE)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                } ?: Intent(Intent.ACTION_MAIN).apply {
                    setPackage(PackageRegistry.WHATSAPP_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                LaunchResult(
                    success = true,
                    targetApp = "WhatsApp",
                    isNativeApp = true,
                    message = "WhatsApp open kar diya hai."
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
}
