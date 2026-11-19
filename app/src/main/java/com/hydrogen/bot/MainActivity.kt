package com.hydrogen.bot

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.hydrogen.bot.ai.ActionType
import com.hydrogen.bot.ai.GeminiIntentParser
import com.hydrogen.bot.ai.ParsedIntent
import com.hydrogen.bot.ai.VisionResult
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
import com.hydrogen.bot.data.SupabaseMemoryManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : FragmentActivity(), VoiceListener {

    private lateinit var voiceManager: VoiceManager
    private lateinit var ttsManager: TextToSpeechManager
    private lateinit var authManager: AuthManager
    private lateinit var vaultManager: EncryptedVaultManager
    private lateinit var memoryManager: SupabaseMemoryManager
    private val intentParser by lazy {
        GeminiIntentParser(
            apiKey = BuildConfig.GEMINI_API_KEY,
            hermesUrl = BuildConfig.HERMES_AGENT_URL,
            hermesKey = BuildConfig.HERMES_AGENT_KEY
        )
    }

    // Observable UI State
    private var isListening by mutableStateOf(false)
    private var volumeRms by mutableFloatStateOf(0f)
    private var statusText by mutableStateOf("Hydrogen Ready")
    private var transcriptText by mutableStateOf("")
    private var lastIntent by mutableStateOf<ParsedIntent?>(null)
    private var isLastNativeApp by mutableStateOf(false)
    private var isLoggedIn by mutableStateOf(false)
    private var currentScreen by mutableStateOf("home") // "home", "auth", "vault"
    private var visionResult by mutableStateOf<VisionResult?>(null)

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

    // Bulletproof Native System Voice Dialog Fallback
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

    // Camera Permission Request Handler
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            safeLaunchCamera()
        } else {
            Toast.makeText(this, "Camera permission allow kijiye photo scan ke liye.", Toast.LENGTH_LONG).show()
        }
    }

    // Gallery / Photo Picker Launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        analyzePhotoWithGemini(bitmap)
                    } else {
                        Toast.makeText(this, "Photo load nahi ho paayi.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Photo decode error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Gemini Multimodal Camera Vision Launcher
    private val takePicturePreviewLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            analyzePhotoWithGemini(bitmap)
        } else {
            statusText = "Photo capture cancel hua."
        }
    }

    private fun analyzePhotoWithGemini(bitmap: Bitmap) {
        statusText = "Analyzing photo with Gemini Vision…"
        ttsManager.speak("Photo scan ho rahi hai, ek second.")
        lifecycleScope.launch {
            try {
                val res = intentParser.analyzeImage(bitmap)
                visionResult = res
                if (res != null) {
                    statusText = res.productName
                    ttsManager.speak("${res.productName}. ${res.description}")
                } else {
                    statusText = "Product pehchaan nahi paaye."
                    ttsManager.speak("Product pehchaan nahi paaye. Kripya dobara try karein.")
                }
            } catch (e: Exception) {
                statusText = "Vision analyze error: ${e.localizedMessage}"
                ttsManager.speak("Photo scan me dikkat aayi.")
            }
        }
    }

    private fun openCameraLens() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                safeLaunchCamera()
            } else {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            safeLaunchCamera()
        }
    }

    private fun safeLaunchCamera() {
        try {
            takePicturePreviewLauncher.launch(null)
        } catch (e: Exception) {
            Toast.makeText(this, "Camera open karne me dikkat aayi. Gallery se select karein.", Toast.LENGTH_SHORT).show()
            try {
                pickImageLauncher.launch("image/*")
            } catch (e2: Exception) {
                Toast.makeText(this, "Camera intent unavailable: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openGalleryPicker() {
        try {
            pickImageLauncher.launch("image/*")
        } catch (e: Exception) {
            Toast.makeText(this, "Gallery open nahi ho paayi: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Core Engines
        voiceManager = VoiceManager(this, this)
        ttsManager = TextToSpeechManager(
            context = this,
            elevenLabsApiKey = BuildConfig.ELEVENLABS_API_KEY,
            voiceId = BuildConfig.ELEVENLABS_VOICE_ID
        )
        authManager = AuthManager(this)
        vaultManager = EncryptedVaultManager(this)
        memoryManager = SupabaseMemoryManager(this)

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
                            },
                            hasSavedSession = authManager.currentUser.email.isNotBlank(),
                            onBiometricLogin = {
                                BiometricAuthHelper.promptBiometric(
                                    activity = this@MainActivity,
                                    onSuccess = {
                                        isLoggedIn = true
                                        currentScreen = "home"
                                        ttsManager.speak("Biometric verified. Hydrogen Bot me aapka swaagat hai.")
                                    },
                                    onError = { err ->
                                        Toast.makeText(this@MainActivity, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
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
                            visionResult = visionResult,
                            onMicClick = { toggleListening() },
                            onOpenVision = { openCameraLens() },
                            onPickGallery = { openGalleryPicker() },
                            onClearVision = { visionResult = null },
                            onSearchAmazon = { query ->
                                AppRouter.launchAmazonSearch(this@MainActivity, query)
                            },
                            onSearchFlipkart = { query ->
                                AppRouter.launchFlipkartSearch(this@MainActivity, query)
                            },
                            onSearchBlinkit = { query ->
                                AppRouter.launchBlinkitSearch(this@MainActivity, query)
                            },
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
                                processSpokenCommand(command, isVoiceInput = false)
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

    private fun processSpokenCommand(spokenText: String, isVoiceInput: Boolean = true) {
        statusText = "Hermes Cloud Brain is reasoning…"
        lifecycleScope.launch {
            val memoryContext = memoryManager.getRecentMemoryContext()
            val intent = intentParser.parse(spokenText, visionResult?.productName, memoryContext)
            lastIntent = intent
            statusText = intent.speechFeedback

            // Autonomous Memory Sync: Saves task, plan, and learned preferences into Supabase PostgreSQL
            memoryManager.recordInteraction(authManager.currentUser.email, spokenText, intent)

            // Voice response gate: speak only on voice input; stay silent in text chat mode
            val speak: (String) -> Unit = { text ->
                if (isVoiceInput) {
                    ttsManager.speak(text)
                }
            }

            // Autonomous Action Execution Engine
            when (intent.action) {
                ActionType.SEARCH_AMAZON -> {
                    speak(intent.speechFeedback)
                    val result = AppRouter.launchAmazonSearch(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                    if (!result.success) {
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                ActionType.SEARCH_YOUTUBE -> {
                    speak(intent.speechFeedback)
                    val result = AppRouter.launchYouTubeSearch(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                }
                ActionType.SEARCH_FLIPKART -> {
                    speak(intent.speechFeedback)
                    val result = AppRouter.launchFlipkartSearch(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                }
                ActionType.SEARCH_BLINKIT, ActionType.QUICK_CART_BLINKIT -> {
                    speak(intent.speechFeedback)
                    val result = AppRouter.launchBlinkitSearch(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                    if (!result.success) {
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                ActionType.SEND_WHATSAPP -> {
                    speak(intent.speechFeedback)
                    val result = AppRouter.launchWhatsApp(this@MainActivity, intent.query)
                    isLastNativeApp = result.isNativeApp
                }
                ActionType.MORNING_BRIEFING -> {
                    val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val batLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val greeting = when (hour) {
                        in 5..11 -> "Shubh prabhat"
                        in 12..16 -> "Shubh dopahar"
                        in 17..21 -> "Shubh sandhya"
                        else -> "Shubh ratri"
                    }
                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                    val batMsg = if (batLevel < 25) {
                        "Sir, battery $batLevel% hai, kripya charger connect karein."
                    } else {
                        "Battery $batLevel% hai, sab theek hai."
                    }
                    val speech = "$greeting! Abhi time $timeStr hua hai. $batMsg Hydrogen Bot aapki har voice command ke liye taiyyar hai."
                    statusText = speech
                    speak(speech)
                    isLastNativeApp = false
                }
                ActionType.CAMERA_VISION -> {
                    speak(intent.speechFeedback)
                    openCameraLens()
                    isLastNativeApp = false
                }
                ActionType.BOOK_TICKET_IRCTC -> {
                    speak(intent.speechFeedback)
                    isLastNativeApp = true
                }
                ActionType.OPEN_APP -> {
                    speak(intent.speechFeedback)
                    val result = AppRouter.launchAppByName(this@MainActivity, intent.targetApp)
                    isLastNativeApp = result.isNativeApp
                    if (!result.success) {
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                ActionType.UNIVERSAL_SEARCH -> {
                    speak(intent.speechFeedback)
                    val result = AppRouter.launchUniversalSearch(this@MainActivity, intent.targetApp, intent.query)
                    isLastNativeApp = result.isNativeApp
                    if (!result.success) {
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                ActionType.SET_ALARM -> {
                    val result = AppRouter.launchSetAlarm(this@MainActivity, intent.query.ifBlank { spokenText })
                    val feedback = if (result.success) result.message else intent.speechFeedback
                    statusText = feedback
                    speak(feedback)
                    isLastNativeApp = result.isNativeApp
                    if (!result.success) {
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                ActionType.SET_TIMER -> {
                    val result = AppRouter.launchSetTimer(this@MainActivity, intent.query.ifBlank { spokenText })
                    val feedback = if (result.success) result.message else intent.speechFeedback
                    statusText = feedback
                    speak(feedback)
                    isLastNativeApp = result.isNativeApp
                    if (!result.success) {
                        Toast.makeText(this@MainActivity, result.message, Toast.LENGTH_LONG).show()
                    }
                }
                else -> {
                    speak(intent.speechFeedback)
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
            try {
                val webSearchIntent = Intent(RecognizerIntent.ACTION_WEB_SEARCH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Hydrogen Bot: Boliye…")
                }
                systemVoiceDialogLauncher.launch(webSearchIntent)
            } catch (e2: Exception) {
                statusText = "Voice ready. Kripya dobara tap karein ya quick action chunein."
                Toast.makeText(this, "Mic tap karein ya quick actions use karein.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
        ttsManager.destroy()
    }
}
