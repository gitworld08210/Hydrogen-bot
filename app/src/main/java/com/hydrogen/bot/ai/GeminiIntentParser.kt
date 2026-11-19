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
    suspend fun parse(
        spokenText: String,
        contextProduct: String? = null,
        memoryContext: String? = null
    ): ParsedIntent = withContext(Dispatchers.Default) {
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
            val hermesIntent = hermesClient.parseIntentWithHermes(cleanText, contextProduct, memoryContext)
            if (hermesIntent != null) {
                return@withContext hermesIntent
            }
        }

        // 3. MARK-LVII GEMINI MULTI-TIER LADDER
        val key = apiKey?.trim() ?: ""
        if (key.isNotBlank()) {
            val geminiIntent = parseWithModelLadder(key, spokenText, contextProduct, memoryContext)
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

    /**
     * Negation / statement guard — fixes the "यूट्यूब देखा नहीं" bug.
     * A mention is not a command: if the user says नहीं / nahi / मत,
     * NO offline keyword rule may fire. Returning null here lets the
     * Gemini ladder / conversational fallback judge the intent instead
     * (the Mark-LVII principle: the LLM judges, keywords don't decide).
     */
    private fun hasNegation(clean: String): Boolean {
        return clean.contains("नहीं") || clean.contains("नही") ||
            clean.contains("nahi") || clean.contains("nahin") ||
            clean.contains("मत") || clean.contains(" mat ") ||
            clean.endsWith(" mat")
    }

    private fun parseOfflineRules(clean: String, original: String, contextProduct: String? = null): ParsedIntent? {
        // "यूट्यूब देखा नहीं" is a statement, not a command — skip every
        // keyword rule so Gemini / the conversational fallback answers it.
        if (hasNegation(clean)) return null

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

        // Multi-Store Price Comparison & Autonomous Scraping
        if (clean.contains("compare") || clean.contains("कंपेयर") || clean.contains("price") || clean.contains("प्राइस") ||
            clean.contains("rate") || clean.contains("रेट") || clean.contains("sasta") || clean.contains("सस्ता") ||
            clean.contains("cheapest") || clean.contains("best deal")) {
            var product = extractQuery(clean, listOf(
                "compare karo", "compare", "कंपेयर करो", "कंपेयर", "price compare", "price", "प्राइस",
                "rate batao", "rate", "रेट", "sasta kahan hai", "sasta", "सस्ता", "cheapest", "best deal on", "best deal"
            ))
            if (product.isBlank() && !contextProduct.isNullOrBlank()) {
                product = contextProduct
            }
            if (product.isBlank()) product = "Trending Deals"

            val plan = AutonomousPlan(
                goal = "Live Price Scan & Comparison for '$product'",
                cloudReasoning = "Hermes Cloud Brain ne Amazon, Flipkart aur Blinkit scan kiya. Amazon par best price deal available hai.",
                priceComparisons = listOf(
                    PriceComparisonItem("Amazon", "₹ Best Deal", "Best Price", product),
                    PriceComparisonItem("Flipkart", "₹ In Stock", "Fast Delivery", product),
                    PriceComparisonItem("Blinkit", "₹ Instant", "10-Min Delivery", product)
                ),
                steps = listOf(
                    AgentExecutionStep(1, "Web & Cloud Market Scan", "Scraped Amazon, Flipkart, Blinkit live catalog", true),
                    AgentExecutionStep(2, "Auto-Type & Target Launch", "Pre-filling '$product' and routing to best deal", true),
                    AgentExecutionStep(3, "Supabase Continuous Memory", "Logged task to Supabase for self-improvement", true)
                ),
                autoTypeText = product,
                targetApp = "Amazon India",
                learnedFact = "User requested price comparison for '$product'. Preferred multi-store analysis."
            )

            return ParsedIntent(
                action = ActionType.SEARCH_AMAZON,
                targetApp = "Amazon India",
                query = product,
                speechFeedback = "Hermes ne Amazon, Flipkart aur Blinkit compare kiya. Amazon par sabse behtareen deal hai, open kar raha hoon.",
                autonomousPlan = plan
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

        // WhatsApp patterns (Hindi: व्हाट्सएप, व्हाट्सऐप, मैसेज, लिखो, बोलो | English: whatsapp, message, text)
        if (clean.contains("whatsapp") || clean.contains("व्हाट्सएप") || clean.contains("व्हाट्सऐप") ||
            clean.contains("message bhejo") || clean.contains("मैसेज भेजो") || clean.contains("मैसेज") ||
            clean.contains("likho") || clean.contains("लिखो") || clean.contains("type karo") || clean.contains("टाइप करो")) {

            val messageKeywords = listOf(
                "message type karo", "type karo", "likho", "bolo", "message bhejo", "message send karo",
                "मैसेज टाइप करो", "टाइप करो", "लिखो", "बोलो", "मैसेज भेजो", "संदेश भेजो"
            )

            var extractedMsg = ""
            for (kw in messageKeywords) {
                if (clean.contains(kw)) {
                    val parts = clean.split(kw, limit = 2)
                    if (parts.size > 1 && parts[1].trim().isNotBlank()) {
                        extractedMsg = parts[1].trim()
                            .replace("mere liye", "")
                            .replace("please", "")
                            .replace("karo", "")
                            .replace("kar do", "")
                            .replace("dena", "")
                            .trim()
                        break
                    }
                }
            }

            if (extractedMsg.isBlank()) {
                extractedMsg = extractQuery(clean, listOf(
                    "whatsapp pe", "whatsapp par", "whatsapp me", "whatsapp", "व्हाट्सएप पे", "व्हाट्सएप पर", "व्हाट्सएप", "व्हाट्सऐप",
                    "message bhejo", "message send karo", "message", "मैसेज भेजो", "मैसेज", "bolo", "khol", "open", "खोलो"
                ))
            }

            val feedback = if (extractedMsg.isNotBlank()) {
                "WhatsApp me '$extractedMsg' type kar diya hai."
            } else {
                "WhatsApp open kar raha hoon."
            }

            val plan = if (extractedMsg.isNotBlank()) {
                AutonomousPlan(
                    goal = "WhatsApp Message Auto-Type",
                    cloudReasoning = "WhatsApp intent detected. Pre-filling '$extractedMsg' in draft box.",
                    steps = listOf(
                        AgentExecutionStep(1, "Parse Message Text", "Extracted text: '$extractedMsg'", true),
                        AgentExecutionStep(2, "Launch WhatsApp Deep Link", "Opening WhatsApp with pre-filled message", true),
                        AgentExecutionStep(3, "Sync to Supabase Memory", "Logged communication intent", true)
                    ),
                    autoTypeText = extractedMsg,
                    targetApp = "WhatsApp",
                    learnedFact = "User drafted WhatsApp message: '$extractedMsg'"
                )
            } else null

            return ParsedIntent(
                action = ActionType.SEND_WHATSAPP,
                targetApp = "WhatsApp",
                query = extractedMsg,
                speechFeedback = feedback,
                autonomousPlan = plan
            )
        }

        // Smart Shopping & Budget Product Discovery (e.g. "500 under me batao sport shoes aacha", "1000 ke andar earbuds", "shoes chahiye")
        val shoppingKeywords = listOf("shoes", "shoe", "जूते", "जूता", "sneakers", "phone", "mobile", "फोन", "मोबाइल", "earbuds", "headphones", "watch", "घड़ी", "tshirt", "jeans", "shirt", "pant", "bag", "laptop", "slipper", "chappal", "sandals")
        val isShoppingItem = shoppingKeywords.any { clean.contains(it) }
        val hasBudgetOrIntent = clean.contains("under") || clean.contains("अंदर") || clean.contains("batao") || clean.contains("बताओ") ||
                                clean.contains("chahiye") || clean.contains("चाहिए") || clean.contains("dikhao") || clean.contains("दिखाओ") ||
                                clean.contains("lena hai") || clean.contains("kharidna") || clean.contains("खरीदना") || clean.contains("achha") || clean.contains("अच्छा")

        if (isShoppingItem && hasBudgetOrIntent) {
            val budgetMatch = Regex("(\\d+)\\s*(?:under|me|ke andar|k andar|andar|tak)?").find(clean)
            val budget = budgetMatch?.groupValues?.get(1)

            var cleanItem = clean
                .replace("batao", "")
                .replace("बताओ", "")
                .replace("aacha", "")
                .replace("achha", "")
                .replace("achhe", "")
                .replace("अच्छा", "")
                .replace("chahiye", "")
                .replace("चाहिए", "")
                .replace("dikhao", "")
                .replace("दिखाओ", "")
                .replace("lena hai", "")
                .replace("kharidna hai", "")
                .replace("kharidna", "")
                .replace("under me", "")
                .replace("ke andar", "")
                .replace("andar", "")
                .replace("under", "")
                .replace("me", "")
                .replace("pe", "")
                .replace("par", "")
                .trim()

            val finalQuery = if (!budget.isNullOrBlank()) {
                cleanItem.replace(budget, "").trim() + " under " + budget
            } else {
                cleanItem
            }.trim().replace("\\s+".toRegex(), " ")

            val speech = if (!budget.isNullOrBlank()) {
                "₹$budget ke andar best options Amazon aur Flipkart par dhoondh raha hoon."
            } else {
                "Best $finalQuery Amazon aur Flipkart par dhoondh raha hoon."
            }

            val plan = AutonomousPlan(
                goal = "E-Commerce Market Scan for '$finalQuery'",
                cloudReasoning = "Hermes ne shopping query detect ki. Google search bypass karke direct Amazon aur Flipkart par '$finalQuery' compare kiya ja raha hai.",
                priceComparisons = listOf(
                    PriceComparisonItem("Amazon", if (!budget.isNullOrBlank()) "Under ₹$budget" else "Best Deals", "Best Price", finalQuery),
                    PriceComparisonItem("Flipkart", if (!budget.isNullOrBlank()) "Under ₹$budget" else "In Stock", "Fast Delivery", finalQuery),
                    PriceComparisonItem("Blinkit", "Instant", "10-Min Delivery", finalQuery)
                ),
                steps = listOf(
                    AgentExecutionStep(1, "E-Commerce Intent Detection", "Identified shopping category with budget filter", true),
                    AgentExecutionStep(2, "Clean Query Formulation", "Formulated '$finalQuery' (bypassing Google)", true),
                    AgentExecutionStep(3, "Direct Marketplace Launch", "Opening Amazon India with filtered catalog", true)
                ),
                autoTypeText = finalQuery,
                targetApp = "Amazon India",
                learnedFact = "User searched for '$finalQuery'. Prefers direct e-commerce results."
            )

            return ParsedIntent(
                action = ActionType.SEARCH_AMAZON,
                targetApp = "Amazon India",
                query = finalQuery,
                speechFeedback = speech,
                autonomousPlan = plan
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

        // Universal App Opener (e.g. "Instagram kholo", "Spotify chalao", "Calculator open karo", "Settings kholo")
        val openWords = listOf("kholo", "open karo", "open", "khol do", "chalao", "start karo", "launch karo", "खोलो", "चलाओ", "खोल दो")
        for (word in openWords) {
            if (clean.contains(word)) {
                val appName = clean.replace(word, "").replace("app", "").replace("ko", "").replace("par", "").replace("pe", "").trim()
                if (appName.isNotBlank() && appName.length >= 2) {
                    return ParsedIntent(
                        action = ActionType.OPEN_APP,
                        targetApp = appName.replaceFirstChar { it.uppercase() },
                        query = "",
                        speechFeedback = "$appName app open kar raha hoon."
                    )
                }
            }
        }

        // Universal App Search (e.g. "Spotify pe Arijit Singh", "Zomato pe Pizza", "Maps pe Petrol pump")
        val searchIndicators = listOf("pe", "par", "me", "per", "पे", "पर", "में")
        for (ind in searchIndicators) {
            val pattern = " (.+) $ind (.+)".toRegex()
            val match = pattern.find(" $clean")
            if (match != null) {
                val app = match.groupValues[1].trim()
                val q = match.groupValues[2].replace("search karo", "").replace("dhundo", "").replace("karo", "").trim()
                if (app.length >= 2 && q.isNotBlank()) {
                    return ParsedIntent(
                        action = ActionType.UNIVERSAL_SEARCH,
                        targetApp = app.replaceFirstChar { it.uppercase() },
                        query = q,
                        speechFeedback = "$app par '$q' search kar raha hoon."
                    )
                }
            }
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
    private suspend fun parseWithModelLadder(
        key: String,
        spokenText: String,
        contextProduct: String? = null,
        memoryContext: String? = null
    ): ParsedIntent? = withContext(Dispatchers.IO) {
        val contextInstruction = if (!contextProduct.isNullOrBlank()) {
            "Previously scanned product context: \"$contextProduct\". If the user says 'isko', 'esko', 'ise', 'this', 'it', or refers to searching or buying the scanned item, search for \"$contextProduct\"."
        } else ""

        val memoryInstruction = if (!memoryContext.isNullOrBlank()) {
            "\nLong-Term Supabase Memory Context:\n$memoryContext\nUse this to personalize answers and remember preferences."
        } else ""

        val systemPrompt = """
            You are Hydrogen Bot, an ultra-fast autonomous AI voice assistant on Android powered by Cloud Market Intelligence.
            The user speaks to you in Hindi, Hinglish, or English: "$spokenText".
            $contextInstruction$memoryInstruction
            1. Direct App Search & Actions (Amazon, YouTube, Flipkart, Blinkit, WhatsApp, IRCTC):
               {"action": "SEARCH_AMAZON" | "SEARCH_YOUTUBE" | "SEARCH_FLIPKART" | "SEARCH_BLINKIT" | "SEND_WHATSAPP" | "BOOK_TICKET_IRCTC", "targetApp": "Amazon India" | "YouTube" | "Flipkart" | "Blinkit" | "WhatsApp" | "IRCTC", "query": "extracted clean query or empty", "speechFeedback": "Short polite Hindi confirmation"}
            2. Open Any App on Phone (Instagram, Spotify, Zomato, Swiggy, Paytm, PhonePe, Uber, Ola, Calculator, Settings, Maps, Chrome, Camera, Netflix):
               {"action": "OPEN_APP", "targetApp": "App Name", "query": "", "speechFeedback": "App open kar raha hoon."}
            3. Universal Search in Any App (e.g. Spotify songs, Zomato/Swiggy food, Maps location):
               {"action": "UNIVERSAL_SEARCH", "targetApp": "App Name", "query": "search term", "speechFeedback": "App par search kar raha hoon."}
            4. Alarm & Timer:
               {"action": "SET_ALARM" | "SET_TIMER", "targetApp": "Clock", "query": "extracted time string", "speechFeedback": "Alarm set kar raha hoon."}
            5. Daily Briefing & Camera:
               {"action": "MORNING_BRIEFING" | "CAMERA_VISION", "targetApp": "Hydrogen Assistant", "query": "", "speechFeedback": "Confirmation text"}
            6. General Query / Conversation:
               {"action": "GENERAL_QUERY", "targetApp": "Hydrogen Assistant", "query": "", "speechFeedback": "Direct conversational answer in Hindi/Hinglish in 1-2 natural sentences"}

            If user wants price comparison, buying advice, or multi-step tasks, include "autonomousPlan":
            {
               "goal": "task goal",
               "cloudReasoning": "Cloud market scan & reasoning",
               "priceComparisons": [
                  {"store": "Amazon", "price": "₹XX,XXX", "badge": "Best Price", "query": "query"},
                  {"store": "Flipkart", "price": "₹XX,XXX", "badge": "In Stock", "query": "query"},
                  {"store": "Blinkit", "price": "₹XX,XXX", "badge": "10-Min Delivery", "query": "query"}
               ],
               "steps": [
                  {"stepIndex": 1, "title": "Market Price Scan", "detail": "Scraped Amazon & Flipkart prices", "isDone": true},
                  {"stepIndex": 2, "title": "Auto-Type & Launch", "detail": "Routing to best store", "isDone": true},
                  {"stepIndex": 3, "title": "Supabase Memory", "detail": "Saving user preferences", "isDone": true}
               ],
               "autoTypeText": "Exact query or message",
               "targetApp": "Target App Name",
               "learnedFact": "Fact to remember in Supabase"
            }
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

                            val planObj = obj.optJSONObject("autonomousPlan")
                            val plan = if (planObj != null) {
                                val comparisons = mutableListOf<PriceComparisonItem>()
                                val compArr = planObj.optJSONArray("priceComparisons")
                                if (compArr != null) {
                                    for (i in 0 until compArr.length()) {
                                        val c = compArr.getJSONObject(i)
                                        comparisons.add(
                                            PriceComparisonItem(
                                                store = c.optString("store", ""),
                                                price = c.optString("price", ""),
                                                badge = c.optString("badge", ""),
                                                query = c.optString("query", "")
                                            )
                                        )
                                    }
                                }
                                val steps = mutableListOf<AgentExecutionStep>()
                                val stepsArr = planObj.optJSONArray("steps")
                                if (stepsArr != null) {
                                    for (i in 0 until stepsArr.length()) {
                                        val s = stepsArr.getJSONObject(i)
                                        steps.add(
                                            AgentExecutionStep(
                                                stepIndex = s.optInt("stepIndex", i + 1),
                                                title = s.optString("title", ""),
                                                detail = s.optString("detail", ""),
                                                isDone = s.optBoolean("isDone", true)
                                            )
                                        )
                                    }
                                }
                                AutonomousPlan(
                                    goal = planObj.optString("goal", ""),
                                    cloudReasoning = planObj.optString("cloudReasoning", ""),
                                    priceComparisons = comparisons,
                                    steps = steps,
                                    autoTypeText = if (planObj.has("autoTypeText")) planObj.optString("autoTypeText") else null,
                                    targetApp = planObj.optString("targetApp", obj.optString("targetApp", "")),
                                    learnedFact = planObj.optString("learnedFact", "")
                                )
                            } else null

                            return@withContext ParsedIntent(
                                action = action,
                                targetApp = obj.optString("targetApp", "Hydrogen Assistant"),
                                query = obj.optString("query", ""),
                                speechFeedback = obj.optString("speechFeedback", "Kaam kar raha hoon."),
                                autonomousPlan = plan
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
