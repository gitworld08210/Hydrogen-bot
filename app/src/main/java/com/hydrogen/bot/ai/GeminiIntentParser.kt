package com.hydrogen.bot.ai

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeminiIntentParser(private val apiKey: String? = null) {

    private val generativeModel by lazy {
        apiKey?.takeIf { it.isNotBlank() }?.let {
            GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = it
            )
        }
    }

    /**
     * Parses spoken text into an actionable intent.
     * Uses Fast Offline Rule Engine first (0ms latency), and escalates to Gemini LLM if complex.
     */
    suspend fun parse(spokenText: String): ParsedIntent = withContext(Dispatchers.Default) {
        val cleanText = spokenText.trim().lowercase()

        // 1. FAST LOCAL RULE ENGINE (Hindi / Hinglish / English)
        val offlineIntent = parseOfflineRules(cleanText, spokenText)
        if (offlineIntent != null) {
            return@withContext offlineIntent
        }

        // 2. GEMINI CLOUD LLM (For complex natural language)
        val model = generativeModel
        if (model != null) {
            try {
                return@withContext parseWithGemini(model, spokenText)
            } catch (e: Exception) {
                // Fallback to general query if network fails
            }
        }

        // 3. DEFAULT FALLBACK
        ParsedIntent(
            action = ActionType.GENERAL_QUERY,
            targetApp = "Hydrogen Assistant",
            query = spokenText,
            speechFeedback = "Main aapke liye '$spokenText' dekh raha hoon."
        )
    }

    private fun parseOfflineRules(clean: String, original: String): ParsedIntent? {
        // Amazon search patterns
        if (clean.contains("amazon") || clean.contains("amzon") || clean.contains("amezon")) {
            val query = extractQuery(clean, listOf("amazon pe", "amazon par", "amazon me", "amazon", "search karo", "dhundo", "search for", "buy", "kharido"))
            return ParsedIntent(
                action = ActionType.SEARCH_AMAZON,
                targetApp = "Amazon India",
                query = query.ifEmpty { "products" },
                speechFeedback = "Amazon App par '${query.ifEmpty { "products" }}' search kar raha hoon."
            )
        }

        // YouTube patterns
        if (clean.contains("youtube") || clean.contains("gaana") || clean.contains("song") || clean.contains("video")) {
            val query = extractQuery(clean, listOf("youtube pe", "youtube par", "chalao", "play", "search", "video", "dikhao", "song"))
            return ParsedIntent(
                action = ActionType.SEARCH_YOUTUBE,
                targetApp = "YouTube",
                query = query.ifEmpty { "trending songs" },
                speechFeedback = "YouTube App par '${query.ifEmpty { "trending songs" }}' open kar diya hai."
            )
        }

        // Flipkart patterns
        if (clean.contains("flipkart")) {
            val query = extractQuery(clean, listOf("flipkart pe", "flipkart par", "search karo", "dhundo", "flipkart"))
            return ParsedIntent(
                action = ActionType.SEARCH_FLIPKART,
                targetApp = "Flipkart",
                query = query.ifEmpty { "deals" },
                speechFeedback = "Flipkart App par '${query.ifEmpty { "deals" }}' search khol raha hoon."
            )
        }

        // WhatsApp patterns
        if (clean.contains("whatsapp") || clean.contains("message bhejo")) {
            val query = extractQuery(clean, listOf("whatsapp pe", "whatsapp par", "message bhejo", "bolo"))
            return ParsedIntent(
                action = ActionType.SEND_WHATSAPP,
                targetApp = "WhatsApp",
                query = query,
                speechFeedback = "WhatsApp open kar raha hoon."
            )
        }

        // IRCTC / Ticket booking (Safety Guardrail Active)
        if (clean.contains("irctc") || clean.contains("ticket book") || clean.contains("train ticket")) {
            return ParsedIntent(
                action = ActionType.BOOK_TICKET_IRCTC,
                targetApp = "IRCTC Rail Connect",
                query = original,
                speechFeedback = "IRCTC ticket details fill kar raha hoon. Payment screen par aane se pehle bot ruk jayega.",
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
            .trim()
    }

    private suspend fun parseWithGemini(model: GenerativeModel, spokenText: String): ParsedIntent {
        val prompt = """
            You are Hydrogen Bot's intent parser. The user spoke in Hindi/English: "$spokenText".
            Classify intent and output strict JSON only:
            {
              "action": "SEARCH_AMAZON" | "SEARCH_YOUTUBE" | "SEARCH_FLIPKART" | "SEND_WHATSAPP" | "GENERAL_QUERY",
              "targetApp": "Amazon India" | "YouTube" | "Flipkart" | "WhatsApp" | "General",
              "query": "extracted clean search term",
              "speechFeedback": "Short polite natural Hindi/Hinglish feedback to speak to the user"
            }
        """.trimIndent()

        val response = model.generateContent(prompt)
        val jsonText = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"
        val obj = JSONObject(jsonText)

        val actionStr = obj.optString("action", "GENERAL_QUERY")
        val action = try { ActionType.valueOf(actionStr) } catch (e: Exception) { ActionType.GENERAL_QUERY }

        return ParsedIntent(
            action = action,
            targetApp = obj.optString("targetApp", "Hydrogen Assistant"),
            query = obj.optString("query", spokenText),
            speechFeedback = obj.optString("speechFeedback", "Kaam kar raha hoon.")
        )
    }
}
