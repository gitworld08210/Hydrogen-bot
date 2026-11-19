package com.hydrogen.bot

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.hydrogen.bot.ai.ActionType
import com.hydrogen.bot.ai.GeminiIntentParser
import com.hydrogen.bot.ai.ParsedIntent
import com.hydrogen.bot.auth.AuthManager
import com.hydrogen.bot.router.AppRouter
import com.hydrogen.bot.security.BiometricAuthHelper
import com.hydrogen.bot.security.EncryptedVaultManager
import com.hydrogen.bot.ui.screens.AuthScreen
import com.hydrogen.bot.ui.screens.HomeScreen
import com.hydrogen.bot.ui.screens.VaultScreen
import com.hydrogen.bot.ui.theme.HydrogenBotTheme
import com.hydrogen.bot.voice.TextToSpeechManager
import com.hydrogen.bot.voice.VoiceListener
import com.hydrogen.bot.voice.VoiceManager
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity(), VoiceListener {

    private lateinit var voiceManager: VoiceManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var authManager: AuthManager
    private lateinit var vaultManager: EncryptedVaultManager
    private val intentParser by lazy { GeminiIntentParser(BuildConfig.GEMINI_API_KEY) }

    // Observable UI State
    private var isListening by mutableStateOf(false)
    private var volumeRms by mutableFloatStateOf(0f)
    private var statusText by mutableStateOf("Hydrogen Ready")
    private var transcriptText by mutableStateOf("")
    private var lastIntent by mutableStateOf<ParsedIntent?>(null)
    private var isLastNativeApp by mutableStateOf(false)
    private var isLoggedIn by mutableStateOf(false)
    private var currentScreen by mutableStateOf("home") // "home", "auth", "vault"

    // Microphone Permission Request Handler
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            safeStartListening()
        } else {
            Toast.makeText(this, "Microphone permission zaroori hai voice assistant ke liye.", Toast.LENGTH_SHORT).show()
        }
    }

    // Bulletproof Native System Voice Dialog Fallback (100% crash-free)
    private val systemVoiceDialogLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                onVoiceResult(matches[0])
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Core Engines
        voiceManager = VoiceManager(this, this)
        ttsManager = TextToSpeechManager(this)
        authManager = AuthManager(this)
        vaultManager = EncryptedVaultManager(this)

        isLoggedIn = authManager.currentUser.isLoggedIn
        currentScreen = if (isLoggedIn) "home" else "auth"

        setContent {
            HydrogenBotTheme {
                when (currentScreen) {
                    "auth" -> {
                        AuthScreen(
                            authManager = authManager,
                            onLoginSuccess = {
                                isLoggedIn = true
                                currentScreen = "home"
                                ttsManager.speak("Hydrogen Bot me aapka swaagat hai.")
                            }
                        )
                    }
                    "vault" -> {
                        VaultScreen(
                            vaultManager = vaultManager,
                            onBack = { currentScreen = "home" },
                            onAuthenticateBiometric = { onSuccess ->
                                BiometricAuthHelper.promptBiometric(
                                    activity = this@MainActivity,
                                    onSuccess = onSuccess,
                                    onError = { err ->
                                        Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )
                    }
                    else -> {
                        HomeScreen(
                            isListening = isListening,
                            volumeRms = volumeRms,
                            statusText = statusText,
                            transcriptText = transcriptText,
                            lastIntent = lastIntent,
                            isLastNativeApp = isLastNativeApp,
                            userEmail = authManager.currentUser.email.ifEmpty { "User" },
                            onMicClick = { toggleListening() },
                            onOpenVault = {
                                BiometricAuthHelper.promptBiometric(
                                    activity = this@MainActivity,
                                    onSuccess = { currentScreen = "vault" },
                                    onError = { err ->
                                        Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onQuickCommandClick = { command ->
                                transcriptText = command
                                processSpokenCommand(command)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun toggleListening() {
        try {
            if (isListening) {
                voiceManager.stopListening()
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    safeStartListening()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        } catch (e: Exception) {
            onRequestSystemVoiceDialog(voiceManager.createSpeechIntent())
        }
    }

    private fun safeStartListening() {
        try {
            voiceManager.startListening()
        } catch (e: Exception) {
            onRequestSystemVoiceDialog(voiceManager.createSpeechIntent())
        }
    }

    private fun processSpokenCommand(spokenText: String) {
        statusText = "Hydrogen is reasoning…"
        lifecycleScope.launch {
            val intent = intentParser.parse(spokenText)
            lastIntent = intent
            statusText = intent.speechFeedback

            // Natural voice reply
            ttsManager.speak(intent.speechFeedback)

            // Autonomous Action Execution Engine
            when (intent.action) {
                ActionType.SEARCH_AMAZON -> {
                    val result = AppRouter.launchAmazonSearch(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                    if (!result.success) {
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                ActionType.SEARCH_YOUTUBE -> {
                    val result = AppRouter.launchYouTubeSearch(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                }
                ActionType.SEARCH_FLIPKART -> {
                    val result = AppRouter.launchFlipkartSearch(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                }
                ActionType.SEND_WHATSAPP -> {
                    val result = AppRouter.launchWhatsApp(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                }
                ActionType.BOOK_TICKET_IRCTC -> {
                    isLastNativeApp = true
                }
                else -> {
                    isLastNativeApp = false
                }
            }
        }
    }

    // Voice Listener Callbacks
    override fun onListeningStateChanged(isListening: Boolean) {
        this.isListening = isListening
        statusText = if (isListening) "Listening…" else "Hydrogen Ready"
    }

    override fun onRmsVolumeChanged(rmsdB: Float) {
        volumeRms = rmsdB
    }

    override fun onVoiceResult(recognizedText: String) {
        transcriptText = recognizedText
        processSpokenCommand(recognizedText)
    }

    override fun onVoiceError(errorMsg: String) {
        statusText = errorMsg
    }

    override fun onRequestSystemVoiceDialog(intent: Intent) {
        try {
            systemVoiceDialogLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice recognition available nahi hai.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
        ttsManager.destroy()
    }
}
