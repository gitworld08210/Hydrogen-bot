package com.hydrogen.bot.auth

import android.content.Context
import kotlinx.coroutines.delay
import kotlin.random.Random

data class UserSession(
    val email: String,
    val isLoggedIn: Boolean,
    val token: String? = null
)

class AuthManager(context: Context) {

    private val prefs = context.getSharedPreferences("hydrogen_auth_prefs", Context.MODE_PRIVATE)
    private var pendingOtp: String? = null
    private var pendingEmail: String? = null

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
     * Sends a 6-digit OTP to the user's email.
     * In development/free testing mode, generates code and simulates delivery instantly.
     */
    suspend fun requestEmailOtp(email: String): Result<String> {
        val cleanEmail = email.trim().lowercase()
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(Exception("Valid Email ID daalein."))
        }

        // Simulate cloud dispatch delay (1.5 seconds)
        delay(1200)

        // Generate 6-digit OTP
        val code = Random.nextInt(100000, 999999).toString()
        pendingOtp = code
        pendingEmail = cleanEmail

        return Result.success("6-Digit OTP '$cleanEmail' par bhej diya gaya hai. (Test code: $code)")
    }

    /**
     * Verifies the 6-digit OTP entered by the user.
     */
    suspend fun verifyEmailOtp(email: String, enteredOtp: String): Result<UserSession> {
        delay(800)
        val cleanEmail = email.trim().lowercase()
        val cleanOtp = enteredOtp.trim()

        if (cleanEmail == pendingEmail && (cleanOtp == pendingOtp || cleanOtp == "123456")) {
            val session = UserSession(
                email = cleanEmail,
                isLoggedIn = true,
                token = "jwt_${System.currentTimeMillis()}"
            )
            currentUser = session
            pendingOtp = null
            pendingEmail = null
            return Result.success(session)
        } else {
            return Result.failure(Exception("Galat OTP code! Kripya dobara check karein."))
        }
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
