package com.hydrogen.bot.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

interface VoiceListener {
    fun onListeningStateChanged(isListening: Boolean)
    fun onRmsVolumeChanged(rmsdB: Float)
    fun onVoiceResult(recognizedText: String)
    fun onVoiceError(errorMsg: String)
    fun onRequestSystemVoiceDialog(intent: Intent)
}

class VoiceManager(private val context: Context, private val listener: VoiceListener) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isCurrentlyListening = false

    init {
        ensureRecognizer()
    }

    @Synchronized
    private fun ensureRecognizer(): SpeechRecognizer? {
        if (speechRecognizer != null) return speechRecognizer

        // 1. Check and create default SpeechRecognizer
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
                Log.d("HydrogenBot", "Initialized default SpeechRecognizer successfully")
                return speechRecognizer
            }
        } catch (e: Throwable) {
            Log.w("HydrogenBot", "Default recognizer init failed: ${e.message}")
        }

        // 2. Try explicit Google Speech Recognition component (standard on Google App)
        try {
            val googleComponent = ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
            )
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context, googleComponent).apply {
                setRecognitionListener(createRecognitionListener())
            }
            Log.d("HydrogenBot", "Initialized Google component SpeechRecognizer")
            return speechRecognizer
        } catch (e: Throwable) {
            Log.w("HydrogenBot", "Google component init failed: ${e.message}")
        }

        // 3. Last resort: direct create without availability check
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
            Log.d("HydrogenBot", "Initialized direct SpeechRecognizer")
            return speechRecognizer
        } catch (e: Throwable) {
            Log.e("HydrogenBot", "All SpeechRecognizer creation attempts failed", e)
            speechRecognizer = null
        }

        return null
    }

    fun startListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { startListening() }
            return
        }

        try {
            val recognizer = ensureRecognizer()
            val intent = createSpeechIntent()

            if (recognizer != null) {
                try {
                    // Cancel any stuck previous sessions
                    recognizer.cancel()
                    isCurrentlyListening = true
                    recognizer.startListening(intent)
                    listener.onListeningStateChanged(true)
                } catch (e: Throwable) {
                    Log.e("HydrogenBot", "startListening exception, resetting recognizer", e)
                    resetRecognizer()
                    listener.onRequestSystemVoiceDialog(intent)
                }
            } else {
                listener.onRequestSystemVoiceDialog(intent)
            }
        } catch (e: Throwable) {
            Log.e("HydrogenBot", "startListening fatal error", e)
            listener.onListeningStateChanged(false)
            listener.onRequestSystemVoiceDialog(createSpeechIntent())
        }
    }

    fun stopListening() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { stopListening() }
            return
        }
        try {
            isCurrentlyListening = false
            speechRecognizer?.stopListening()
            listener.onListeningStateChanged(false)
        } catch (e: Throwable) {
            // Ignore
        }
    }

    private fun resetRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Throwable) {}
        speechRecognizer = null
        ensureRecognizer()
    }

    fun destroy() {
        try {
            isCurrentlyListening = false
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Throwable) {
            // Ignore
        }
    }

    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hydrogen Bot sun raha hai… Boliye")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                mainHandler.post {
                    listener.onListeningStateChanged(true)
                }
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                mainHandler.post {
                    listener.onRmsVolumeChanged(rmsdB)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                mainHandler.post {
                    listener.onListeningStateChanged(false)
                }
            }

            override fun onError(error: Int) {
                mainHandler.post {
                    listener.onListeningStateChanged(false)
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Kuch sunayi nahi diya, kripya dobara bolein."
                        SpeechRecognizer.ERROR_NETWORK -> "Network issue mila."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        SpeechRecognizer.ERROR_CLIENT -> {
                            resetRecognizer()
                            "Voice ready, kripya dobara tap karein."
                        }
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                            resetRecognizer()
                            "Voice engine busy tha, reset ho gaya."
                        }
                        else -> "Voice recognition code $error."
                    }
                    listener.onVoiceError(msg)
                }
            }

            override fun onResults(results: Bundle?) {
                mainHandler.post {
                    listener.onListeningStateChanged(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        listener.onVoiceResult(matches[0])
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                mainHandler.post {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        listener.onVoiceResult(matches[0])
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
}
