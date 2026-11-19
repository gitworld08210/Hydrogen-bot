package com.hydrogen.bot.data

import android.content.Context
import android.util.Log
import com.hydrogen.bot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ChatPersona(
    val userName: String = "User",
    val tone: String = "Casual friendly Hinglish",
    val frequentWords: List<String> = emptyList(),
    val commonEmojis: List<String> = emptyList(),
    val sampleSentences: List<String> = emptyList(),
    val isCalibrated: Boolean = false
)

/**
 * ChatPersonaManager:
 * Analyzes WhatsApp & Instagram exported chat histories, learns the user's authentic
 * vocabulary, slang, emojis, and sentence style, and stores the calibrated persona in Supabase
 * so Hermes Cloud Brain / Gemini can chat on the user's behalf with authentic personality.
 */
class ChatPersonaManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("hydrogen_chat_persona", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

    fun getPersona(): ChatPersona {
        val isCalibrated = prefs.getBoolean("is_calibrated", false)
        val userName = prefs.getString("user_name", "User") ?: "User"
        val tone = prefs.getString("tone", "Casual friendly Hinglish") ?: "Casual friendly Hinglish"
        val frequentWordsStr = prefs.getString("frequent_words", "haan, bhai, arre, theek hai, bro, sahi hai, yaar") ?: ""
        val emojisStr = prefs.getString("common_emojis", "😂, 👍, 🙏, 🔥") ?: "😂, 👍, 🙏, 🔥"
        val sampleSentencesStr = prefs.getString("sample_sentences", "haan bhai, theek hai main dekhta hoon, arre kal milte hain") ?: ""

        val words = frequentWordsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val emojis = emojisStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val samples = sampleSentencesStr.split(";").map { it.trim() }.filter { it.isNotBlank() }

        return ChatPersona(
            userName = userName,
            tone = tone,
            frequentWords = words,
            commonEmojis = emojis,
            sampleSentences = samples,
            isCalibrated = isCalibrated
        )
    }

    /**
     * Builds a detailed system prompt instruction for Hermes / Gemini
     * to respond precisely in the user's genuine writing style.
     */
    fun getPersonaSystemPrompt(): String {
        val p = getPersona()
        val wordsText = if (p.frequentWords.isNotEmpty()) p.frequentWords.take(12).joinToString(", ") else "haan, bhai, arre, theek hai, bro, sahi hai"
        val emojisText = if (p.commonEmojis.isNotEmpty()) p.commonEmojis.take(6).joinToString(" ") else "😂 👍"
        val samplesText = if (p.sampleSentences.isNotEmpty()) p.sampleSentences.take(4).joinToString("\n- ") else "haan bhai theek hai\narre kal milte hain"

        return """
You are acting as an AI representative responding to an incoming personal chat on the user's behalf.
IMPORTANT: Respond EXACTLY in the user's personal voice, tone, and typing habits:
- Tone: ${p.tone}. Never sound like an AI assistant, robot, or formal customer support.
- Natural Hinglish slang & favorite words to use: $wordsText
- Typical emojis used by user: $emojisText
- Real style samples from user's chat history:
- $samplesText
Keep the reply concise, natural, friendly, and human (1-2 sentences maximum, matching casual WhatsApp/Instagram conversation).
""".trimIndent()
    }

    /**
     * Parses an exported WhatsApp or Instagram chat transcript (.txt format),
     * extracts sender statistics, vocabulary, emojis, and uploads the Persona to Supabase.
     */
    suspend fun calibrateFromChatExport(
        rawContent: String,
        userEmail: String?
    ): ChatPersona = withContext(Dispatchers.IO) {
        val lines = rawContent.lines()
        val senderMap = mutableMapOf<String, MutableList<String>>()

        // WhatsApp export regex: "12/05/23, 14:30 - Sender: message" or "[12/05/23, 14:30:00] Sender: message"
        val waRegex = Regex("(?:\\[?\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4},?\\s+\\d{1,2}:\\d{2}(?::\\d{2})?\\s*(?:AM|PM|am|pm)?\\]?\\s*[-:]?\\s*)([^:]+):\\s*(.+)")

        for (line in lines) {
            val match = waRegex.find(line.trim())
            if (match != null) {
                val sender = match.groupValues[1].trim()
                val msg = match.groupValues[2].trim()
                if (!msg.contains("<Media omitted>") && !msg.contains("Messages and calls are end-to-end encrypted")) {
                    senderMap.getOrPut(sender) { mutableListOf() }.add(msg)
                }
            }
        }

        // Pick the sender with messages (or highest count if multiple)
        val targetSenderEntry = senderMap.maxByOrNull { it.value.size }
        val targetSender = targetSenderEntry?.key ?: "User"
        val userMessages = targetSenderEntry?.value ?: lines.filter { it.isNotBlank() && it.length > 2 }

        // 1. Extract emojis
        val emojiRegex = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+")
        val emojiCount = mutableMapOf<String, Int>()
        for (msg in userMessages) {
            emojiRegex.findAll(msg).forEach { m ->
                val emoji = m.value
                emojiCount[emoji] = (emojiCount[emoji] ?: 0) + 1
            }
        }
        val topEmojis = emojiCount.entries.sortedByDescending { it.value }.take(8).map { it.key }

        // 2. Extract frequent slang / vocabulary words
        val stopWords = setOf(
            "the", "is", "at", "which", "on", "a", "an", "this", "that", "it",
            "hai", "ho", "ka", "ki", "ke", "ko", "se", "me", "pe", "par", "bhi",
            "to", "aur", "kuch", "kar", "karo", "raha", "rahe", "nahi", "nahin", "mat"
        )
        val wordCount = mutableMapOf<String, Int>()
        for (msg in userMessages) {
            val words = msg.lowercase().split(Regex("[^a-zA-Z0-9\u0900-\u097F]+"))
            for (w in words) {
                if (w.length >= 3 && !stopWords.contains(w) && !w.all { it.isDigit() }) {
                    wordCount[w] = (wordCount[w] ?: 0) + 1
                }
            }
        }
        val topWords = wordCount.entries.sortedByDescending { it.value }.take(15).map { it.key }

        // 3. Representative sample sentences
        val sampleSentences = userMessages
            .filter { it.length in 8..65 && !it.startsWith("http") }
            .distinct()
            .take(6)

        val persona = ChatPersona(
            userName = targetSender,
            tone = "Authentic natural Hinglish (${topWords.take(4).joinToString(", ")})",
            frequentWords = if (topWords.isNotEmpty()) topWords else listOf("haan", "bhai", "arre", "theek", "bro"),
            commonEmojis = if (topEmojis.isNotEmpty()) topEmojis else listOf("😂", "👍", "🔥"),
            sampleSentences = if (sampleSentences.isNotEmpty()) sampleSentences else listOf("haan bhai kal milte hain", "theek hai dekh loon"),
            isCalibrated = true
        )

        // Save locally in SharedPreferences
        prefs.edit()
            .putBoolean("is_calibrated", true)
            .putString("user_name", persona.userName)
            .putString("tone", persona.tone)
            .putString("frequent_words", persona.frequentWords.joinToString(", "))
            .putString("common_emojis", persona.commonEmojis.joinToString(", "))
            .putString("sample_sentences", persona.sampleSentences.joinToString("; "))
            .apply()

        // Sync to Supabase Cloud Memory (hydrogen_memories table)
        syncToSupabase(userEmail, persona)

        persona
    }

    private fun syncToSupabase(userEmail: String?, persona: ChatPersona) {
        if (!supabaseUrl.contains("supabase.co") || supabaseKey.isBlank() || supabaseKey == "your-anon-key") return

        try {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
            val learnedSummary = "User Chat Persona Calibrated: Slangs=[${persona.frequentWords.take(8).joinToString()}], Emojis=[${persona.commonEmojis.joinToString()}], Samples=[${persona.sampleSentences.take(2).joinToString()}]"

            val json = JSONObject().apply {
                put("user_email", userEmail ?: "anonymous_user")
                put("user_command", "IMPORT_CHAT_STYLE")
                put("agent_action", "PERSONA_SYNC")
                put("agent_plan", "Calibrated authentic typing persona from real conversations.")
                put("learned_fact", learnedSummary)
                put("created_at", nowIso)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/hydrogen_memories")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Prefer", "return=minimal")
                .post(body)
                .build()

            client.newCall(request).execute().close()
            Log.i("ChatPersonaManager", "Successfully synced Chat Persona to Supabase Cloud.")
        } catch (e: Exception) {
            Log.w("ChatPersonaManager", "Failed to sync persona to Supabase: ${e.message}")
        }
    }
}
