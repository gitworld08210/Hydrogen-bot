package com.hydrogen.bot.data

import android.content.Context
import android.util.Log
import com.hydrogen.bot.BuildConfig
import com.hydrogen.bot.ai.AutonomousPlan
import com.hydrogen.bot.ai.ParsedIntent
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

/**
 * SupabaseMemoryManager:
 * Manages long-term conversational memory and continuous self-improvement in Supabase PostgreSQL.
 * Persists user requests, cloud reasoning plans, price comparisons, and learned preferences.
 */
class SupabaseMemoryManager(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val prefs = context.getSharedPreferences("hydrogen_bot_memory", Context.MODE_PRIVATE)
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

    /**
     * Saves user command and agent execution plan to Supabase PostgreSQL table 'hydrogen_memories'
     * and local cache for instant retrieval.
     */
    suspend fun recordInteraction(
        userEmail: String?,
        command: String,
        intent: ParsedIntent
    ) = withContext(Dispatchers.IO) {
        val plan = intent.autonomousPlan
        val learnedFact = plan?.learnedFact ?: "User executed ${intent.action} for ${intent.query.ifBlank { intent.targetApp }}"
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        val recordObj = JSONObject().apply {
            put("user_email", userEmail ?: "anonymous_user")
            put("user_command", command)
            put("agent_action", intent.action.name)
            put("agent_plan", plan?.cloudReasoning ?: intent.speechFeedback)
            put("learned_fact", learnedFact)
            put("created_at", nowIso)
        }

        // 1. Save to local fast cache (up to 20 recent memories)
        try {
            val localArrayStr = prefs.getString("local_memories", "[]") ?: "[]"
            val localArray = JSONArray(localArrayStr)
            localArray.put(recordObj)
            while (localArray.length() > 20) {
                localArray.remove(0)
            }
            prefs.edit().putString("local_memories", localArray.toString()).apply()
        } catch (e: Exception) {
            Log.w("HydrogenMemory", "Local cache update failed: ${e.message}")
        }

        // 2. Synchronize to Supabase Cloud Database (REST API)
        if (supabaseUrl.contains("supabase.co") && supabaseKey.isNotBlank() && supabaseKey != "your-anon-key") {
            try {
                val body = recordObj.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$supabaseUrl/rest/v1/hydrogen_memories")
                    .header("apikey", supabaseKey)
                    .header("Authorization", "Bearer $supabaseKey")
                    .header("Prefer", "return=minimal")
                    .post(body)
                    .build()

                val resp = client.newCall(request).execute()
                if (resp.isSuccessful) {
                    Log.i("HydrogenMemory", "Synced memory successfully to Supabase.")
                } else {
                    Log.w("HydrogenMemory", "Supabase memory HTTP ${resp.code}: ${resp.body?.string()}")
                }
            } catch (e: Exception) {
                Log.w("HydrogenMemory", "Supabase sync non-blocking error: ${e.message}")
            }
        }
    }

    /**
     * Returns a concise summary of recent memories to feed into Hermes Agent & Gemini
     * so the AI remembers past preferences, habits, and shopping choices.
     */
    fun getRecentMemoryContext(): String {
        return try {
            val localArrayStr = prefs.getString("local_memories", "[]") ?: "[]"
            val localArray = JSONArray(localArrayStr)
            if (localArray.length() == 0) return ""

            val builder = StringBuilder("User Memory & Past Context:\n")
            val count = Math.min(5, localArray.length())
            val startIndex = localArray.length() - count
            for (i in startIndex until localArray.length()) {
                val item = localArray.getJSONObject(i)
                val cmd = item.optString("user_command", "")
                val fact = item.optString("learned_fact", "")
                if (cmd.isNotBlank()) {
                    builder.append("- Command: \"$cmd\" | Learned: $fact\n")
                }
            }
            builder.toString().trim()
        } catch (e: Exception) {
            ""
        }
    }
}
