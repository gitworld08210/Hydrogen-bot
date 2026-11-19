package com.hydrogen.bot.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
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
 * Dual-Engine Ultra-Realistic Speech Synthesis.
 * Primary: ElevenLabs AI Neural Voice (Real Human Voice in Hindi/English).
 * Fallback: Android Native TextToSpeech Engine (0ms, Offline).
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

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("en", "IN"))
            }
            tts?.setPitch(1.0f)
            tts?.setSpeechRate(1.05f)
            isReady = true
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return

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
                    put("stability", 0.5)
                    put("similarity_boost", 0.8)
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
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.w("HydrogenBot", "ElevenLabs TTS exception: ${e.message}, falling back to native TTS")
            false
        }
    }

    private fun speakNative(text: String) {
        if (isReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "HydrogenTTS")
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
