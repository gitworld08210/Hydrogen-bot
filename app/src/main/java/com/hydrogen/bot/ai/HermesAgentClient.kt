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
        contextProduct: String? = null,
        memoryContext: String? = null
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

        val memoryInfo = if (!memoryContext.isNullOrBlank()) {
            "\nLong-Term Supabase Memory & Past Context:\n$memoryContext\nUse this memory to personalize responses and remember preferences."
        } else ""

        val systemPrompt = """
            You are Hydrogen Bot Brain powered by Hermes Agent & Cloud Web Intelligence.
            Parse the user's spoken request into an action JSON with autonomous multi-step instructions and price comparison where applicable.
            $contextInfo$memoryInfo

            Supported Actions:
            1. Open Any App on Phone (Instagram, Spotify, Zomato, Swiggy, Uber, Ola, Paytm, PhonePe, Maps, Calculator, Settings, Chrome, Twitter, Telegram, etc.):
               {"action": "OPEN_APP", "targetApp": "Exact App Name", "query": "", "speechFeedback": "{App} open kar raha hoon."}
            2. Universal App Search / Action in any app:
               {"action": "UNIVERSAL_SEARCH", "targetApp": "Target App Name", "query": "search query or task", "speechFeedback": "{App} par search kar raha hoon."}
            3. Direct App Search & Shopping (Amazon, YouTube, Flipkart, Blinkit, WhatsApp, IRCTC):
               {"action": "SEARCH_AMAZON" | "SEARCH_YOUTUBE" | "SEARCH_FLIPKART" | "SEARCH_BLINKIT" | "SEND_WHATSAPP" | "BOOK_TICKET_IRCTC", "targetApp": "Amazon India" | "YouTube" | "Flipkart" | "Blinkit" | "WhatsApp" | "IRCTC", "query": "product/song/contact name", "speechFeedback": "Short natural Hindi confirmation"}
            4. Daily Briefing:
               {"action": "MORNING_BRIEFING", "targetApp": "Hydrogen Assistant", "query": "", "speechFeedback": "Daily briefing generate kar raha hoon."}
            5. Camera Vision:
               {"action": "CAMERA_VISION", "targetApp": "Hydrogen Vision", "query": "", "speechFeedback": "Camera open kar raha hoon."}
            6. Alarm & Timer:
               {"action": "SET_ALARM" | "SET_TIMER", "targetApp": "Clock", "query": "extracted time string", "speechFeedback": "Alarm set kar raha hoon."}
            7. General Query / Conversation:
               {"action": "GENERAL_QUERY", "targetApp": "Hydrogen Assistant", "query": "", "speechFeedback": "Direct conversational answer in Hindi/Hinglish in 1-2 natural sentences"}

            If the user asks to compare prices, buy products, or do multi-step tasks, provide "autonomousPlan":
            {
               "goal": "task goal",
               "cloudReasoning": "Hermes cloud reasoning & web price scan explanation",
               "priceComparisons": [
                  {"store": "Amazon", "price": "₹XX,XXX", "badge": "Best Price", "query": "clean query"},
                  {"store": "Flipkart", "price": "₹XX,XXX", "badge": "Available", "query": "clean query"},
                  {"store": "Blinkit", "price": "₹XX,XXX", "badge": "10-Min Delivery", "query": "clean query"}
               ],
               "steps": [
                  {"stepIndex": 1, "title": "Cloud Price Scan", "detail": "Scraped Amazon & Flipkart prices", "isDone": true},
                  {"stepIndex": 2, "title": "Auto-Type & Launch", "detail": "Routing to best store", "isDone": true},
                  {"stepIndex": 3, "title": "Supabase Memory", "detail": "Saving user preferences", "isDone": true}
               ],
               "autoTypeText": "Exact text to type or search",
               "targetApp": "Recommended App Name",
               "learnedFact": "Key fact learned to remember in Supabase"
            }

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

                val planObj = obj.optJSONObject("autonomousPlan")
                val plan = if (planObj != null) {
                    val comparisons = mutableListOf<PriceComparisonItem>()
                    val compArr = planObj.optJSONArray("priceComparisons")
                    if (compArr != null) {
                        for (i in 0 until compArr.length()) {
                            val c = compArr.getJSONObject(i)
                            comparisons.add(
                                PriceComparisonItem(
                                    store = c.optString("store", ""),
                                    price = c.optString("price", ""),
                                    badge = c.optString("badge", ""),
                                    query = c.optString("query", "")
                                )
                            )
                        }
                    }
                    val steps = mutableListOf<AgentExecutionStep>()
                    val stepsArr = planObj.optJSONArray("steps")
                    if (stepsArr != null) {
                        for (i in 0 until stepsArr.length()) {
                            val s = stepsArr.getJSONObject(i)
                            steps.add(
                                AgentExecutionStep(
                                    stepIndex = s.optInt("stepIndex", i + 1),
                                    title = s.optString("title", ""),
                                    detail = s.optString("detail", ""),
                                    isDone = s.optBoolean("isDone", true)
                                )
                            )
                        }
                    }
                    AutonomousPlan(
                        goal = planObj.optString("goal", ""),
                        cloudReasoning = planObj.optString("cloudReasoning", ""),
                        priceComparisons = comparisons,
                        steps = steps,
                        autoTypeText = if (planObj.has("autoTypeText")) planObj.optString("autoTypeText") else null,
                        targetApp = planObj.optString("targetApp", obj.optString("targetApp", "")),
                        learnedFact = planObj.optString("learnedFact", "")
                    )
                } else null

                return@withContext ParsedIntent(
                    action = action,
                    targetApp = obj.optString("targetApp", "Hydrogen Assistant"),
                    query = obj.optString("query", ""),
                    speechFeedback = obj.optString("speechFeedback", "Kaam kar raha hoon."),
                    autonomousPlan = plan
                )
            }
        } catch (e: Exception) {
            Log.w("HydrogenBot", "Hermes Agent call failed: ${e.message}. Falling back to local/Gemini.")
        }
        null
    }
}
