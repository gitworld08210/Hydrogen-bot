package com.hydrogen.bot.voice

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
        initRecognizerSafe()
    }

    private fun initRecognizerSafe() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                } else {
                    Log.w("HydrogenBot", "SpeechRecognizer not directly available on device")
                }
            } catch (e: Throwable) {
                Log.e("HydrogenBot", "Error initializing SpeechRecognizer", e)
                speechRecognizer = null
            }
        }
    }

    fun startListening() {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizerSafe()
                }

                val intent = createSpeechIntent()

                if (speechRecognizer != null) {
                    isCurrentlyListening = true
                    speechRecognizer?.startListening(intent)
                    listener.onListeningStateChanged(true)
                } else {
                    // Fallback to Native System Voice Dialog (100% crash-free on all Androids)
                    listener.onRequestSystemVoiceDialog(intent)
                }
            } catch (e: Throwable) {
                Log.e("HydrogenBot", "Mic start error, falling back to system dialog", e)
                listener.onListeningStateChanged(false)
                // Fallback to System Voice Dialog
                listener.onRequestSystemVoiceDialog(createSpeechIntent())
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                isCurrentlyListening = false
                speechRecognizer?.stopListening()
                listener.onListeningStateChanged(false)
            } catch (e: Throwable) {
                // Ignore safe
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                isCurrentlyListening = false
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Throwable) {
                // Ignore safe
            }
        }
    }

    fun createSpeechIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hydrogen Bot is listening… Boliye")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
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
                        SpeechRecognizer.ERROR_CLIENT -> "Voice client issue, system dialog open ho raha hai."
                        else -> "Voice recognition error (Code $error)."
                    }

                    // On client error or engine disconnect, fall back to native system dialog
                    if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        listener.onRequestSystemVoiceDialog(createSpeechIntent())
                    } else {
                        listener.onVoiceError(msg)
                    }
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
