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
    SET_ALARM,
    SET_TIMER,
    OPEN_APP,
    UNIVERSAL_SEARCH,
    SCROLL_DOWN,
    SCROLL_UP,
    PHONE_HOME,
    PHONE_BACK,
    PHONE_RECENTS,
    PHONE_NOTIFICATIONS,
    AUTO_CLICK,
    GENERAL_QUERY
}

data class PriceComparisonItem(
    val store: String,
    val price: String,
    val badge: String = "",
    val query: String = ""
)

data class AgentExecutionStep(
    val stepIndex: Int,
    val title: String,
    val detail: String,
    val isDone: Boolean = true
)

data class AutonomousPlan(
    val goal: String,
    val cloudReasoning: String,
    val priceComparisons: List<PriceComparisonItem> = emptyList(),
    val steps: List<AgentExecutionStep> = emptyList(),
    val autoTypeText: String? = null,
    val targetApp: String = "",
    val learnedFact: String = ""
)

data class ParsedIntent(
    val action: ActionType,
    val targetApp: String,
    val query: String,
    val speechFeedback: String,
    val requiresSafetyPause: Boolean = false,
    val details: Map<String, String> = emptyMap(),
    val autonomousPlan: AutonomousPlan? = null
)
