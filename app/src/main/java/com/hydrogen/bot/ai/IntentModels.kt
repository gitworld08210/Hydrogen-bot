package com.hydrogen.bot.ai

enum class ActionType {
    SEARCH_AMAZON,
    SEARCH_YOUTUBE,
    SEARCH_FLIPKART,
    SEARCH_BLINKIT,
    SEND_WHATSAPP,
    BOOK_TICKET_IRCTC,
    QUICK_CART_BLINKIT,
    CAB_COMPARE,
    PAYMENT_PAUSE,
    DEVICE_CONTROL,
    MORNING_BRIEFING,
    CAMERA_VISION,
    GENERAL_QUERY
}

data class ParsedIntent(
    val action: ActionType,
    val targetApp: String,
    val query: String,
    val speechFeedback: String,
    val requiresSafetyPause: Boolean = false,
    val details: Map<String, String> = emptyMap()
)
