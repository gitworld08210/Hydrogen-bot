package com.hydrogen.bot.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class VisionResult(
    val productName: String,
    val description: String,
    val amazonQuery: String,
    val flipkartQuery: String
)

class GeminiIntentParser(
    private val apiKey: String? = null,
    private val hermesUrl: String? = null,
    private val hermesKey: String? = null
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val hermesClient = if (!hermesUrl.isNullOrBlank()) HermesAgentClient(hermesUrl, hermesKey) else null

    // Mark-LVII Verified Model Ladder (Ultra-low latency first)
    private val modelLadder = listOf(
        "gemini-3.5-flash-lite",
        "gemini-flash-lite-latest",
        "gemini-3.8-flash"
    )

    /**
     * Parses spoken text into an actionable intent with Contextual Pronoun Resolution.
     * Uses Fast Offline Rule Engine first (0ms latency, Hindi & English),
     * and escalates to Hermes Cloud Agent (Render) -> Mark-LVII Gemini Model Ladder if needed.
     */
    suspend fun parse(spokenText: String, contextProduct: String? = null): ParsedIntent = withContext(Dispatchers.Default) {
        var cleanText = spokenText.trim().lowercase()

        // Contextual Pronoun Resolution (e.g. "isko search karo blinkit pe" -> "Butter search karo blinkit pe")
        if (!contextProduct.isNullOrBlank()) {
            val pronouns = listOf("isko", "esko", "ise", "isse", "yeh", "ye", "this", "it", "is product ko", "isko bhi")
            for (p in pronouns) {
                if (cleanText.contains(p)) {
                    cleanText = cleanText.replace(p, contextProduct.lowercase())
                }
            }
        }

        // 1. FAST LOCAL RULE ENGINE (Hindi Devanagari & English/Hinglish)
        val offlineIntent = parseOfflineRules(cleanText, spokenText, contextProduct)
        if (offlineIntent != null) {
            return@withContext offlineIntent
        }

        // 2. HERMES CLOUD AGENT (RENDER.COM)
        if (hermesClient?.isConfigured == true) {
            val hermesIntent = hermesClient.parseIntentWithHermes(cleanText, contextProduct)
            if (hermesIntent != null) {
                return@withContext hermesIntent
            }
        }

        // 3. MARK-LVII GEMINI MULTI-TIER LADDER
        val key = apiKey?.trim() ?: ""
        if (key.isNotBlank()) {
            val geminiIntent = parseWithModelLadder(key, spokenText, contextProduct)
            if (geminiIntent != null) {
                return@withContext geminiIntent
            }
        }

        // 4. SMART CONVERSATIONAL FALLBACK
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
                "Main sun raha hoon. Aap Amazon, YouTube, Flipkart, Blinkit, WhatsApp ya koi sawaal pooch sakte hain."
        }

        ParsedIntent(
            action = ActionType.GENERAL_QUERY,
            targetApp = "Hydrogen Assistant",
            query = "",
            speechFeedback = conversationalReply
        )
    }

    private fun parseOfflineRules(clean: String, original: String, contextProduct: String? = null): ParsedIntent? {
        // Morning Briefing / Battery Check
        if (clean.contains("good morning") || clean.contains("गुड मॉर्निंग") || clean.contains("सुप्रभात") ||
            clean.contains("briefing") || clean.contains("ब्रीफिंग") || clean.contains("battery") || clean.contains("बैटरी")) {
            return ParsedIntent(
                action = ActionType.MORNING_BRIEFING,
                targetApp = "Hydrogen Assistant",
                query = "",
                speechFeedback = "Daily briefing generate kar raha hoon."
            )
        }

        // Camera / Vision Scan
        if (clean.contains("camera") || clean.contains("कैमरा") || clean.contains("photo") || clean.contains("फोटो") ||
            clean.contains("scan") || clean.contains("स्कैन") || clean.contains("ye kya hai") || clean.contains("yeh kya hai")) {
            return ParsedIntent(
                action = ActionType.CAMERA_VISION,
                targetApp = "Hydrogen Vision",
                query = "",
                speechFeedback = "Camera open kar raha hoon, product frame me rakhein."
            )
        }

        // Native Alarm (Hindi: अलार्म, अलारम, जगा देना | English: alarm, wake me up)
        if (clean.contains("alarm") || clean.contains("अलार्म") || clean.contains("अलारम") ||
            clean.contains("wake me up") || clean.contains("jaga dena") || clean.contains("जगा देना")) {
            return ParsedIntent(
                action = ActionType.SET_ALARM,
                targetApp = "Clock",
                query = original,
                speechFeedback = "Alarm set kar raha hoon."
            )
        }

        // Native Timer (Hindi: टाइमर | English: timer)
        if (clean.contains("timer") || clean.contains("टाइमर")) {
            return ParsedIntent(
                action = ActionType.SET_TIMER,
                targetApp = "Clock",
                query = original,
                speechFeedback = "Timer start kar raha hoon."
            )
        }

        // Blinkit patterns (Hindi: ब्लिंकिट, ग्रोफर्स | English: blinkit, grofers, blnkit)
        if (clean.contains("blinkit") || clean.contains("ब्लिंकिट") || clean.contains("blnkit") ||
            clean.contains("blink it") || clean.contains("grofers") || clean.contains("ग्रोफर्स")) {
            var query = extractQuery(clean, listOf(
                "blinkit pe", "blinkit par", "blinkit me", "blinkit", "ब्लिंकिट पे", "ब्लिंकिट पर", "ब्लिंकिट",
                "blink it pe", "blink it", "grofers pe", "grofers", "ग्रोफर्स",
                "search karo", "dhundo", "mangwao", "order karo", "search for", "charc karo", "खोजो", "ढूंढो", "मंगवाओ"
            ))
            if (query.isBlank() && !contextProduct.isNullOrBlank()) {
                query = contextProduct
            }
            return ParsedIntent(
                action = ActionType.SEARCH_BLINKIT,
                targetApp = "Blinkit",
                query = query,
                speechFeedback = if (query.isEmpty()) "Blinkit App open kar raha hoon." else "Blinkit App par '$query' search kar raha hoon."
            )
        }

        // YouTube patterns (Hindi: यूट्यूब, यू ट्यूब, गाना, गाने, वीडियो | English: youtube, song, video)
        if (clean.contains("youtube") || clean.contains("यूट्यूब") || clean.contains("यू ट्यूब") ||
            clean.contains("gaana") || clean.contains("गाना") || clean.contains("गाने") ||
            clean.contains("song") || clean.contains("video") || clean.contains("वीडियो")) {
            var query = extractQuery(clean, listOf(
                "youtube pe", "youtube par", "youtube me", "youtube", "यूट्यूब पे", "यूट्यूब पर", "यूट्यूब", "यू ट्यूब",
                "chalao", "play", "search", "video", "dikhao", "song", "gaana", "gaane", "sunao", "खोल", "चलाओ", "दिखाओ", "गाना"
            ))
            if (query.isBlank() && !contextProduct.isNullOrBlank()) {
                query = contextProduct
            }
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
            var query = extractQuery(clean, listOf(
                "amazon pe", "amazon par", "amazon me", "amazon", "अमेज़न पे", "अमेज़न पर", "अमेज़न", "अमेजॉन", "अमेजन",
                "search karo", "dhundo", "search for", "buy", "kharido", "खोजो", "ढूंढो", "खरीदो"
            ))
            if (query.isBlank() && !contextProduct.isNullOrBlank()) {
                query = contextProduct
            }
            return ParsedIntent(
                action = ActionType.SEARCH_AMAZON,
                targetApp = "Amazon India",
                query = query,
                speechFeedback = if (query.isEmpty()) "Amazon App open kar raha hoon." else "Amazon App par '$query' search kar raha hoon."
            )
        }

        // Flipkart patterns (Hindi: फ्लिपकार्ट | English: flipkart, flipcart)
        if (clean.contains("flipkart") || clean.contains("फ्लिपकार्ट") || clean.contains("flipcart")) {
            var query = extractQuery(clean, listOf(
                "flipkart pe", "flipkart par", "flipkart me", "flipkart", "फ्लिपकार्ट पे", "फ्लिपकार्ट पर", "फ्लिपकार्ट",
                "search karo", "dhundo", "kharido", "deals", "खोजो", "ढूंढो"
            ))
            if (query.isBlank() && !contextProduct.isNullOrBlank()) {
                query = contextProduct
            }
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
    private suspend fun parseWithModelLadder(key: String, spokenText: String, contextProduct: String? = null): ParsedIntent? = withContext(Dispatchers.IO) {
        val contextInstruction = if (!contextProduct.isNullOrBlank()) {
            "Previously scanned product context: \"$contextProduct\". If the user says 'isko', 'esko', 'ise', 'this', 'it', or refers to searching or buying the scanned item, search for \"$contextProduct\"."
        } else ""

        val systemPrompt = """
            You are Hydrogen Bot, an ultra-fast autonomous AI voice assistant on Android.
            The user speaks to you in Hindi, Hinglish, or English: "$spokenText".
            $contextInstruction
            1. If the user wants to open or search an app (Amazon, YouTube, Flipkart, Blinkit, WhatsApp, IRCTC):
               {"action": "SEARCH_AMAZON" | "SEARCH_YOUTUBE" | "SEARCH_FLIPKART" | "SEARCH_BLINKIT" | "SEND_WHATSAPP" | "BOOK_TICKET_IRCTC", "targetApp": "Amazon India" | "YouTube" | "Flipkart" | "Blinkit" | "WhatsApp" | "IRCTC", "query": "extracted clean query or empty", "speechFeedback": "Short polite Hindi confirmation (e.g. 'Blinkit par dhoondh raha hoon.')"}
            2. If user asks for morning briefing or battery:
               {"action": "MORNING_BRIEFING", "targetApp": "Hydrogen Assistant", "query": "", "speechFeedback": "Daily briefing generate kar raha hoon."}
            3. If user wants camera or scan:
               {"action": "CAMERA_VISION", "targetApp": "Hydrogen Vision", "query": "", "speechFeedback": "Camera open kar raha hoon."}
            4. If user wants to set an alarm or timer:
               {"action": "SET_ALARM" | "SET_TIMER", "targetApp": "Clock", "query": "extracted time e.g. '05:00' or '10 minutes'", "speechFeedback": "Alarm set kar raha hoon."}
            5. Otherwise, for general questions, greetings, or conversation:
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

    /**
     * Gemini Vision Product Analyzer:
     * Analyzes captured photo to identify product and generate Amazon & Flipkart search queries.
     */
    suspend fun analyzeImage(bitmap: Bitmap): VisionResult? = withContext(Dispatchers.IO) {
        val key = apiKey?.trim() ?: ""
        if (key.isBlank()) return@withContext null

        try {
            val maxDim = 800
            val scale = (maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)).coerceAtMost(1f)
            val scaledBmp = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            val stream = ByteArrayOutputStream()
            scaledBmp.compress(Bitmap.CompressFormat.JPEG, 80, stream)
            val base64Img = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val visionPrompt = """
                Identify the product or object in this photo.
                Output strict JSON only:
                {
                  "productName": "Exact Brand and Product Name (e.g. Nike Air Max, Boat Airdopes, Fastrack Watch)",
                  "description": "Short 1-sentence polite Hindi description",
                  "amazonQuery": "Clean keyword to search on Amazon India",
                  "flipkartQuery": "Clean keyword to search on Flipkart"
                }
            """.trimIndent()

            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", visionPrompt))
                        put(JSONObject().put("inline_data", JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", base64Img)
                        }))
                    })
                }))
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())

            for (m in modelLadder) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent?key=$key"
                    val request = Request.Builder().url(url).post(requestBody).build()
                    val response = client.newCall(request).execute()
                    val respStr = response.body?.string() ?: ""

                    if (response.isSuccessful && respStr.isNotBlank()) {
                        val root = JSONObject(respStr)
                        val candidates = root.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val text = candidates.getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                                .replace("```json", "")
                                .replace("```", "")
                                .trim()
                            val obj = JSONObject(text)
                            return@withContext VisionResult(
                                productName = obj.optString("productName", "Pehchana hua product"),
                                description = obj.optString("description", "Product pehchaan liya gaya hai."),
                                amazonQuery = obj.optString("amazonQuery", ""),
                                flipkartQuery = obj.optString("flipkartQuery", "")
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w("HydrogenBot", "Vision ladder error on $m: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("HydrogenBot", "Vision analysis failed", e)
        }

        null
    }
}
