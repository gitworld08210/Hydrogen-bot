package com.hydrogen.bot.auth

import android.content.Context
import com.hydrogen.bot.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UserSession(
    val email: String,
    val isLoggedIn: Boolean,
    val token: String? = null
)

class AuthManager(context: Context) {

    private val prefs = context.getSharedPreferences("hydrogen_auth_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val supabaseKey = BuildConfig.SUPABASE_ANON_KEY

    var currentUser: UserSession
        get() {
            val email = prefs.getString("user_email", null)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)
            val token = prefs.getString("auth_token", null)
            return UserSession(email ?: "", isLoggedIn, token)
        }
        private set(value) {
            prefs.edit()
                .putString("user_email", value.email)
                .putBoolean("is_logged_in", value.isLoggedIn)
                .putString("auth_token", value.token)
                .apply()
        }

    /**
     * Sends a real Email OTP via Supabase Auth only.
     * Supabase generates, sends and verifies the code — the app never
     * creates or accepts a local code. Email delivery is Supabase's
     * configured SMTP (Resend SMTP in the Supabase dashboard).
     * Endpoint: POST {SUPABASE_URL}/auth/v1/otp
     */
    suspend fun requestEmailOtp(email: String): Result<String> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return@withContext Result.failure(Exception("Kripya valid Email ID daalein."))
        }

        if (!supabaseUrl.contains("supabase.co") || supabaseKey.isBlank() || supabaseKey == "your-anon-key") {
            return@withContext Result.failure(Exception("Supabase configure nahi hai. Kripya app settings check karein."))
        }

        try {
            val json = JSONObject().apply {
                put("email", cleanEmail)
                put("create_user", true)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/otp")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return@withContext Result.success("OTP aapke email '$cleanEmail' par bhej diya gaya hai. Kripya apna Inbox ya Spam check karein.")
            } else {
                val err = response.body?.string() ?: ""
                android.util.Log.e("HydrogenBot", "Supabase OTP request failed: $err")
                return@withContext Result.failure(Exception("OTP bhejne me problem hui. Kripya dobara try karein."))
            }
        } catch (e: Exception) {
            android.util.Log.e("HydrogenBot", "Supabase OTP error", e)
            return@withContext Result.failure(Exception("Network problem. Kripya internet check karke dobara try karein."))
        }
    }

    /**
     * Verifies the Email OTP code against Supabase Auth only.
     * No local code is ever accepted.
     */
    suspend fun verifyEmailOtp(email: String, enteredOtp: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanOtp = enteredOtp.trim()

        if (!supabaseUrl.contains("supabase.co") || supabaseKey.isBlank() || supabaseKey == "your-anon-key") {
            return@withContext Result.failure(Exception("Supabase configure nahi hai. Kripya app settings check karein."))
        }

        try {
            val json = JSONObject().apply {
                put("type", "email")
                put("email", cleanEmail)
                put("token", cleanOtp)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/auth/v1/verify")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val respStr = response.body?.string() ?: ""
            if (response.isSuccessful) {
                val respJson = JSONObject(respStr)
                val accessToken = respJson.optString("access_token", "")
                if (accessToken.isBlank()) {
                    return@withContext Result.failure(Exception("Session token nahi mila. Kripya dobara try karein."))
                }
                val session = UserSession(email = cleanEmail, isLoggedIn = true, token = accessToken)
                currentUser = session
                syncUserToSupabase(cleanEmail, accessToken)
                return@withContext Result.success(session)
            } else {
                android.util.Log.w("HydrogenBot", "Supabase verify returned: $respStr")
                return@withContext Result.failure(Exception("Galat ya expired OTP code! Kripya email par aaya 6-digit code enter karein."))
            }
        } catch (e: Exception) {
            android.util.Log.e("HydrogenBot", "Supabase verify error", e)
            return@withContext Result.failure(Exception("Network problem. Kripya internet check karke dobara try karein."))
        }
    }

    /**
     * Syncs user record to Supabase PostgreSQL database.
     * Table: 'users' (columns: email, last_active, assistant)
     */
    private suspend fun syncUserToSupabase(email: String, token: String) = withContext(Dispatchers.IO) {
        if (!supabaseUrl.contains("supabase.co") || supabaseKey == "your-anon-key") return@withContext
        try {
            val userRecord = JSONObject().apply {
                put("email", email)
                put("last_active", System.currentTimeMillis())
                put("assistant", "Hydrogen Bot")
            }
            val body = userRecord.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$supabaseUrl/rest/v1/users")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer $supabaseKey")
                .header("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()
            client.newCall(request).execute()
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
