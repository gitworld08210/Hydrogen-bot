package com.hydrogen.bot

import android.app.Application
import android.util.Log

class HydrogenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Global Crash Guard to prevent silent closes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("HydrogenBot", "Global Uncaught Exception in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
