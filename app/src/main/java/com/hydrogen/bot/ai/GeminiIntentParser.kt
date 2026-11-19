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

class GeminiIntentParser(private val apiKey: String? = null) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    // Mark-LVII Verified Model Ladder (Ultra-low latency first)
    private val modelLadder = listOf(
        "gemini-3.5-flash-lite",
        "gemini-flash-lite-latest",
        "gemini-3.8-flash"
    )

    /**
     * Parses spoken text into an actionable intent.
     * Uses Fast Offline Rule Engine first (0ms latency, Hindi & English),
     * and escalates to Mark-LVII Gemini Model Ladder if needed.
     */
    suspend fun parse(spokenText: String): ParsedIntent = withContext(Dispatchers.Default) {
        val cleanText = spokenText.trim().lowercase()

        // 1. FAST LOCAL RULE ENGINE (Hindi Devanagari & English/Hinglish)
        val offlineIntent = parseOfflineRules(cleanText, spokenText)
        if (offlineIntent != null) {
            return@withContext offlineIntent
        }

        // 2. MARK-LVII GEMINI MULTI-TIER LADDER
        val key = apiKey?.trim() ?: ""
        if (key.isNotBlank()) {
            val geminiIntent = parseWithModelLadder(key, spokenText)
            if (geminiIntent != null) {
                return@withContext geminiIntent
            }
        }

        // 3. SMART CONVERSATIONAL FALLBACK (No repetitive robotic text)
        val conversationalReply = when {
            cleanText.contains("kaise ho") || cleanText.contains("kaisa hai") || cleanText.contains("how are you") ->
                "Main ekdum badhiya hoon! Bataiye, aaj main aapki kya madad karoon?"
            cleanText.contains("naam kya hai") || cleanText.contains("who are you") || cleanText.contains("kaun ho") ->
                "Main Hydrogen Bot hoon, aapka autonomous AI voice assistant!"
            cleanText.contains("namaste") || cleanText.contains("hello") || cleanText.contains("hi") || cleanText.contains("hey") ->
                "Namaste! Bataiye kya search ya open karna hai?"
            cleanText.contains("shukriya") || cleanText.contains("dhanyawad") || cleanText.contains("thank") ->
                "Aapka swaagat hai! Hamesha aapki seva me haazir hoon."
            else ->
                "Main sun raha hoon. Aap Amazon, YouTube, Flipkart, WhatsApp ya koi sawaal pooch sakte hain."
        }

        ParsedIntent(
            action = ActionType.GENERAL_QUERY,
            targetApp = "Hydrogen Assistant",
            query = "",
            speechFeedback = conversationalReply
        )
    }

    private fun parseOfflineRules(clean: String, original: String): ParsedIntent? {
        // YouTube patterns (Hindi: यूट्यूब, यू ट्यूब, गाना, गाने, वीडियो | English: youtube, song, video)
        if (clean.contains("youtube") || clean.contains("यूट्यूब") || clean.contains("यू ट्यूब") ||
            clean.contains("gaana") || clean.contains("गाना") || clean.contains("गाने") ||
            clean.contains("song") || clean.contains("video") || clean.contains("वीडियो")) {
            val query = extractQuery(clean, listOf(
                "youtube pe", "youtube par", "youtube me", "youtube", "यूट्यूब पे", "यूट्यूब पर", "यूट्यूब", "यू ट्यूब",
                "chalao", "play", "search", "video", "dikhao", "song", "gaana", "gaane", "sunao", "खोल", "चलाओ", "दिखाओ", "गाना"
            ))
            return ParsedIntent(
                action = ActionType.SEARCH_YOUTUBE,
                targetApp = "YouTube",
                query = query,
                speechFeedback = if (query.isEmpty()) "YouTube App open kar raha hoon." else "YouTube App par '$query' play kar raha hoon."
            )
        }

        // Amazon patterns (Hindi: अमेज़न, अमेजॉन, अमेजन | English: amazon, amzon, amezon)
        if (clean.contains("amazon") || clean.contains("अमेज़न") || clean.contains("अमेजॉन") || clean.contains("अमेजन") ||
            clean.contains("amzon") || clean.contains("amezon")) {
            val query = extractQuery(clean, listOf(
                "amazon pe", "amazon par", "amazon me", "amazon", "अमेज़न पे", "अमेज़न पर", "अमेज़न", "अमेजॉन", "अमेजन",
                "search karo", "dhundo", "search for", "buy", "kharido", "खोजो", "ढूंढो", "खरीदो"
            ))
            return ParsedIntent(
                action = ActionType.SEARCH_AMAZON,
                targetApp = "Amazon India",
                query = query,
                speechFeedback = if (query.isEmpty()) "Amazon App open kar raha hoon." else "Amazon App par '$query' search kar raha hoon."
            )
        }

        // Flipkart patterns (Hindi: फ्लिपकार्ट | English: flipkart, flipcart)
        if (clean.contains("flipkart") || clean.contains("फ्लिपकार्ट") || clean.contains("flipcart")) {
            val query = extractQuery(clean, listOf(
                "flipkart pe", "flipkart par", "flipkart me", "flipkart", "फ्लिपकार्ट पे", "फ्लिपकार्ट पर", "फ्लिपकार्ट",
                "search karo", "dhundo", "kharido", "deals", "खोजो", "ढूंढो"
            ))
            return ParsedIntent(
                action = ActionType.SEARCH_FLIPKART,
                targetApp = "Flipkart",
                query = query,
                speechFeedback = if (query.isEmpty()) "Flipkart App open kar raha hoon." else "Flipkart App par '$query' search khol raha hoon."
            )
        }

        // WhatsApp patterns (Hindi: व्हाट्सएप, व्हाट्सऐप, मैसेज | English: whatsapp, message)
        if (clean.contains("whatsapp") || clean.contains("व्हाट्सएप") || clean.contains("व्हाट्सऐप") ||
            clean.contains("message bhejo") || clean.contains("मैसेज")) {
            val query = extractQuery(clean, listOf(
                "whatsapp pe", "whatsapp par", "whatsapp", "व्हाट्सएप पे", "व्हाट्सएप पर", "व्हाट्सएप", "व्हाट्सऐप",
                "message bhejo", "bolo", "khol", "open", "मैसेज भेजो", "मैसेज"
            ))
            return ParsedIntent(
                action = ActionType.SEND_WHATSAPP,
                targetApp = "WhatsApp",
                query = query,
                speechFeedback = "WhatsApp open kar raha hoon."
            )
        }

        // IRCTC / Ticket booking (Safety Guardrail Active)
        if (clean.contains("irctc") || clean.contains("आईआरसीटीसी") || clean.contains("ticket book") ||
            clean.contains("train ticket") || clean.contains("ट्रेन टिकट") || clean.contains("टिकट")) {
            return ParsedIntent(
                action = ActionType.BOOK_TICKET_IRCTC,
                targetApp = "IRCTC Rail Connect",
                query = original,
                speechFeedback = "IRCTC ticket details open kar raha hoon. Payment screen par aane se pehle bot ruk jayega.",
                requiresSafetyPause = true
            )
        }

        return null
    }

    private fun extractQuery(text: String, stopWords: List<String>): String {
        var result = text
        for (word in stopWords) {
            result = result.replace(word, "")
        }
        return result.replace("mere liye", "")
            .replace("please", "")
            .replace("karo", "")
            .replace("kar do", "")
            .replace("khol do", "")
            .replace("kholo", "")
            .replace("चलाओ", "")
            .replace("खोलो", "")
            .replace("दिखाओ", "")
            .replace("करो", "")
            .trim()
    }

    /**
     * Mark-LVII Multi-Model Ladder:
     * Walks through the models (gemini-3.5-flash-lite -> gemini-flash-lite-latest -> gemini-3.8-flash).
     */
    private suspend fun parseWithModelLadder(key: String, spokenText: String): ParsedIntent? = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are Hydrogen Bot, an ultra-fast autonomous AI voice assistant on Android.
            The user speaks to you in Hindi, Hinglish, or English: "$spokenText".
            1. If the user wants to open or search an app (Amazon, YouTube, Flipkart, WhatsApp, IRCTC):
               {"action": "SEARCH_AMAZON" | "SEARCH_YOUTUBE" | "SEARCH_FLIPKART" | "SEND_WHATSAPP" | "BOOK_TICKET_IRCTC", "targetApp": "Amazon India" | "YouTube" | "Flipkart" | "WhatsApp" | "IRCTC", "query": "extracted clean query or empty", "speechFeedback": "Short polite Hindi confirmation (e.g. 'YouTube khol raha hoon.')"}
            2. Otherwise, for general questions, greetings, or conversation:
               {"action": "GENERAL_QUERY", "targetApp": "Hydrogen Assistant", "query": "", "speechFeedback": "Direct conversational answer in Hindi/Hinglish in 1-2 natural sentences"}
            Output strict JSON only.
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", systemPrompt)
                }))
            }))
        }
        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

        for (modelName in modelLadder) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val respStr = response.body?.string() ?: ""

                if (response.isSuccessful && respStr.isNotBlank()) {
                    val root = JSONObject(respStr)
                    val candidates = root.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val content = firstCandidate.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val rawText = parts.getJSONObject(0).optString("text", "")
                            val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
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
                    }
                } else {
                    Log.w("HydrogenBot", "Model $modelName returned ${response.code}: $respStr. Stepping to next rung...")
                }
            } catch (e: Exception) {
                Log.w("HydrogenBot", "Model $modelName exception: ${e.message}. Stepping to next rung...")
            }
        }

        null
    }
}
