package com.hydrogen.bot.router

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

object ContactLookupHelper {
    private const val TAG = "ContactLookupHelper"

    /**
     * Looks up contact's phone number by name from device contacts.
     */
    fun findPhoneNumberByName(context: Context, contactName: String): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CONTACTS permission not granted.")
            return null
        }
        val cleanName = contactName.trim().lowercase()
        if (cleanName.isBlank()) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$cleanName%")

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                var bestMatchNumber: String? = null
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)?.lowercase()?.trim() ?: ""
                    val number = cursor.getString(numberIndex)
                    if (number != null) {
                        // Exact match priority
                        if (name == cleanName) {
                            return cleanPhoneNumber(number)
                        }
                        // First prefix or partial match fallback
                        if (bestMatchNumber == null) {
                            bestMatchNumber = cleanPhoneNumber(number)
                        }
                    }
                }
                bestMatchNumber
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up contact '$contactName': ${e.message}")
            null
        }
    }

    fun cleanPhoneNumber(raw: String): String {
        var digits = raw.replace("[^0-9+]".toRegex(), "")
        if (digits.startsWith("+")) {
            digits = digits.substring(1)
        }
        // If 10-digit Indian number without country code, add 91
        if (digits.length == 10) {
            digits = "91$digits"
        }
        return digits
    }
}
