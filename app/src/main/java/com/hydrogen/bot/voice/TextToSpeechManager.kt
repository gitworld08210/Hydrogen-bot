package com.hydrogen.bot.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * TextToSpeechManager:
 * Soft, natural, conversational speech synthesis.
 * Primary: ElevenLabs AI Neural Voice (if API key provided).
 * Secondary: Google High-Quality Soft Neural Voice (hi-IN / en-IN, calm tone, soft pitch).
 */
class TextToSpeechManager(
    private val context: Context,
    private val elevenLabsApiKey: String? = null,
    private val voiceId: String? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isReady = false
    private var mediaPlayer: MediaPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var onSpeechDoneCallback: (() -> Unit)? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setupSoftNeuralVoice()
            setupUtteranceListener()
            isReady = true
        } else {
            Log.w("HydrogenTTS", "TextToSpeech init failed with status: $status")
        }
    }

    /**
     * Finds and applies the softest, most natural neural voice available on the device.
     * Prefers Google Hindi/Indian English high-quality neural voices.
     */
    private fun setupSoftNeuralVoice() {
        try {
            val availableVoices = tts?.voices ?: emptySet()
            
            // Priority 1: Soft female/male neural voices for Indian languages
            val preferredVoice = availableVoices.firstOrNull { v ->
                val name = v.name.lowercase()
                name.contains("hi-in-x-hid") ||  // Google Hindi Soft Female Neural
                name.contains("hi-in-x-hie") ||  // Google Hindi Soft Male Neural
                name.contains("en-in-x-end") ||  // Google English India Soft Female Neural
                name.contains("en-in-x-enc") ||  // Google English India Soft Male Neural
                name.contains("hi-in-x-hia") ||
                name.contains("hi_in") ||
                name.contains("en_in")
            } ?: availableVoices.firstOrNull { it.locale.language == "hi" }
              ?: availableVoices.firstOrNull { it.locale.country == "IN" }

            if (preferredVoice != null) {
                tts?.voice = preferredVoice
                tts?.language = preferredVoice.locale
                Log.i("HydrogenTTS", "Selected soft neural voice: ${preferredVoice.name}")
            } else {
                val result = tts?.setLanguage(Locale("hi", "IN"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale("en", "IN"))
                }
            }

            // Calibrated for a gentle, polite, human conversational tone
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.92f) // Slightly slower, calm, soft pace
        } catch (e: Exception) {
            Log.w("HydrogenTTS", "Voice setup fallback: ${e.message}")
            tts?.setLanguage(Locale("hi", "IN"))
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(0.95f)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                coroutineScope.launch(Dispatchers.Main) {
                    onSpeechDoneCallback?.invoke()
                    onSpeechDoneCallback = null
                }
            }

            override fun onError(utteranceId: String?) {
                coroutineScope.launch(Dispatchers.Main) {
                    onSpeechDoneCallback = null
                }
            }
        })
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (text.isBlank()) {
            onDone?.invoke()
            return
        }

        this.onSpeechDoneCallback = onDone

        val cleanKey = elevenLabsApiKey?.trim() ?: ""
        if (cleanKey.isNotBlank()) {
            coroutineScope.launch {
                val success = speakWithElevenLabs(text, cleanKey)
                if (!success) {
                    speakNative(text)
                }
            }
        } else {
            speakNative(text)
        }
    }

    private suspend fun speakWithElevenLabs(text: String, apiKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val selectedVoice = if (!voiceId.isNullOrBlank()) voiceId.trim() else "21m00Tcm4TlvDq8ikWAM"
            val url = "https://api.elevenlabs.io/v1/text-to-speech/$selectedVoice?output_format=mp3_44100_128"

            val jsonBody = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.6)
                    put("similarity_boost", 0.85)
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("HydrogenBot", "ElevenLabs TTS failed: HTTP ${response.code}")
                return@withContext false
            }

            val audioBytes = response.body?.bytes() ?: return@withContext false
            val tempFile = File(context.cacheDir, "elevenlabs_temp.mp3")
            FileOutputStream(tempFile).use { it.write(audioBytes) }

            withContext(Dispatchers.Main) {
                stop()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .build()
                    )
                    setDataSource(tempFile.absolutePath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        it.release()
                        mediaPlayer = null
                        onSpeechDoneCallback?.invoke()
                        onSpeechDoneCallback = null
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.w("HydrogenBot", "ElevenLabs TTS exception: ${e.message}, falling back to native soft voice")
            false
        }
    }

    private fun speakNative(text: String) {
        if (isReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HydrogenTTS_${System.currentTimeMillis()}")
        }
    }

    fun stop() {
        try {
            tts?.stop()
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            onSpeechDoneCallback = null
        } catch (e: Exception) {
            // Safe cleanup
        }
    }

    fun destroy() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
