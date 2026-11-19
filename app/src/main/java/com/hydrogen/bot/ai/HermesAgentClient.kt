package com.hydrogen.bot.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * HermesAgentClient:
 * Connects Hydrogen Bot directly to the Cloud Hermes Agent running on Render.com.
 * Uses OpenAI-compatible /v1/chat/completions endpoint.
 */
class HermesAgentClient(
    private val baseUrl: String?,
    private val apiKey: String?
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val isConfigured: Boolean
        get() = !baseUrl.isNullOrBlank()

    suspend fun parseIntentWithHermes(
        spokenText: String,
        contextProduct: String? = null
    ): ParsedIntent? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null

        val cleanUrl = baseUrl!!.trim().trimEnd('/')
        val endpoint = if (cleanUrl.endsWith("/v1/chat/completions")) {
            cleanUrl
        } else if (cleanUrl.endsWith("/v1")) {
            "$cleanUrl/chat/completions"
        } else {
            "$cleanUrl/v1/chat/completions"
        }

        val contextInfo = if (!contextProduct.isNullOrBlank()) {
            "Previously scanned product context: \"$contextProduct\". If user says 'isko', 'esko', 'this', 'it', use this product."
        } else ""

        val systemPrompt = """
            You are Hydrogen Bot Brain powered by Hermes Agent.
            Parse the user's spoken request into an action JSON.
            $contextInfo
            Supported Actions:
            1. App search & actions:
               {"action": "SEARCH_AMAZON" | "SEARCH_YOUTUBE" | "SEARCH_FLIPKART" | "SEARCH_BLINKIT" | "SEND_WHATSAPP" | "BOOK_TICKET_IRCTC", "targetApp": "Amazon India" | "YouTube" | "Flipkart" | "Blinkit" | "WhatsApp" | "IRCTC", "query": "product/song/contact name", "speechFeedback": "Short natural Hindi confirmation (e.g. 'Blinkit par dhoondh raha hoon.')"}
            2. Daily Briefing:
               {"action": "MORNING_BRIEFING", "targetApp": "Hydrogen Assistant", "query": "", "speechFeedback": "Daily briefing generate kar raha hoon."}
            3. Camera Vision:
               {"action": "CAMERA_VISION", "targetApp": "Hydrogen Vision", "query": "", "speechFeedback": "Camera open kar raha hoon."}
            4. Alarm & Timer:
               {"action": "SET_ALARM" | "SET_TIMER", "targetApp": "Clock", "query": "extracted time string", "speechFeedback": "Alarm set kar raha hoon."}
            5. General Query / Conversation:
               {"action": "GENERAL_QUERY", "targetApp": "Hydrogen Assistant", "query": "", "speechFeedback": "Direct conversational answer in Hindi/Hinglish in 1-2 natural sentences"}
            Output strict JSON only without markdown formatting.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("model", "hermes-agent")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", spokenText)
                })
            })
            put("temperature", 0.2)
        }

        try {
            val reqBuilder = Request.Builder()
                .url(endpoint)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

            if (!apiKey.isNullOrBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer ${apiKey.trim()}")
            }

            val response = client.newCall(reqBuilder.build()).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.w("HydrogenBot", "Hermes Agent returned HTTP ${response.code}: $respBody")
                return@withContext null
            }

            val root = JSONObject(respBody)
            val choices = root.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val message = choices.getJSONObject(0).optJSONObject("message")
                val content = message?.optString("content", "") ?: ""
                val cleanJson = content.replace("```json", "").replace("```", "").trim()
                val obj = JSONObject(cleanJson)

                val actionStr = obj.optString("action", "GENERAL_QUERY")
                val action = try { ActionType.valueOf(actionStr) } catch (e: Exception) { ActionType.GENERAL_QUERY }

                return@withContext ParsedIntent(
                    action = action,
                    targetApp = obj.optString("targetApp", "Hydrogen Assistant"),
                    query = obj.optString("query", ""),
                    speechFeedback = obj.optString("speechFeedback", "Kaam kar raha hoon.")
                )
            }
        } catch (e: Exception) {
            Log.w("HydrogenBot", "Hermes Agent call failed: ${e.message}. Falling back to local/Gemini.")
        }
        null
    }
}
