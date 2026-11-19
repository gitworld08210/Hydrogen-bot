package com.hydrogen.bot.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class VaultItem(
    val id: String,
    val title: String,
    val usernameOrEmail: String,
    val secretValue: String, // Encrypted password, pin, or address
    val category: String     // "App Login", "IRCTC Passenger", "Delivery Address"
)

class EncryptedVaultManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        "hydrogen_secure_vault",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredential(item: VaultItem) {
        val serialized = "${item.id}|${item.title}|${item.usernameOrEmail}|${item.secretValue}|${item.category}"
        securePrefs.edit().putString(item.id, serialized).apply()
    }

    fun getCredential(id: String): VaultItem? {
        val raw = securePrefs.getString(id, null) ?: return null
        val parts = raw.split("|")
        return if (parts.size >= 5) {
            VaultItem(
                id = parts[0],
                title = parts[1],
                usernameOrEmail = parts[2],
                secretValue = parts[3],
                category = parts[4]
            )
        } else null
    }

    fun getAllItems(): List<VaultItem> {
        return securePrefs.all.values.mapNotNull {
            val raw = it as? String ?: return@mapNotNull null
            val parts = raw.split("|")
            if (parts.size >= 5) {
                VaultItem(parts[0], parts[1], parts[2], parts[3], parts[4])
            } else null
        }
    }

    fun deleteItem(id: String) {
        securePrefs.edit().remove(id).apply()
    }
}
