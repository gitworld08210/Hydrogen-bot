package com.hydrogen.bot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrogen.bot.ai.ActionType
import com.hydrogen.bot.ai.ParsedIntent
import com.hydrogen.bot.ui.theme.CanvasWhite
import com.hydrogen.bot.ui.theme.HairlineBorder
import com.hydrogen.bot.ui.theme.HydrogenBlue
import com.hydrogen.bot.ui.theme.HydrogenPillBg
import com.hydrogen.bot.ui.theme.SlateDark
import com.hydrogen.bot.ui.theme.SlateMuted
import com.hydrogen.bot.ui.theme.SlateSecondary

/**
 * ActionCard:
 * Clean, elegant, natural assistant result card.
 * Displays the action, soft feedback, and clean message/search details without fake gimmicks.
 */
@Composable
fun ActionCard(
    intent: ParsedIntent,
    isNativeApp: Boolean,
    statusText: String,
    modifier: Modifier = Modifier,
    onStoreClick: ((String, String) -> Unit)? = null
) {
    val (appIcon, appColor) = when (intent.action) {
        ActionType.SEARCH_AMAZON -> Icons.Default.ShoppingCart to Color(0xFFFF9900)
        ActionType.SEARCH_FLIPKART -> Icons.Default.ShoppingCart to Color(0xFF2874F0)
        ActionType.SEARCH_BLINKIT -> Icons.Default.ShoppingCart to Color(0xFFF8CB46)
        ActionType.SEARCH_YOUTUBE -> Icons.Default.PlayArrow to Color(0xFFFF0000)
        ActionType.SEARCH_GOOGLE -> Icons.Default.Search to Color(0xFF4285F4)
        ActionType.SEND_WHATSAPP -> Icons.Default.Chat to Color(0xFF25D366)
        ActionType.OPEN_APP -> Icons.Default.SmartToy to Color(0xFF0F172A)
        ActionType.UNIVERSAL_SEARCH -> Icons.Default.Search to Color(0xFF0284C7)
        ActionType.SET_ALARM, ActionType.SET_TIMER -> Icons.Default.Alarm to Color(0xFF8B5CF6)
        ActionType.CAMERA_VISION -> Icons.Default.CameraAlt to Color(0xFF0D9488)
        else -> Icons.Default.SmartToy to HydrogenBlue
    }

    val actionBadgeText = when (intent.action) {
        ActionType.OPEN_APP -> "APP LAUNCH"
        ActionType.SEND_WHATSAPP -> "WHATSAPP"
        ActionType.SEARCH_GOOGLE -> "GOOGLE SEARCH"
        ActionType.SEARCH_YOUTUBE -> "YOUTUBE"
        ActionType.SEARCH_AMAZON, ActionType.SEARCH_FLIPKART, ActionType.SEARCH_BLINKIT -> "SHOPPING"
        ActionType.UNIVERSAL_SEARCH -> "IN-APP SEARCH"
        ActionType.GENERAL_QUERY -> "CONVERSATION"
        else -> if (isNativeApp) "NATIVE" else "READY"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CanvasWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Clean Target App Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(appColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = appIcon,
                            contentDescription = null,
                            tint = appColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = intent.targetApp.ifBlank { "Hydrogen Assistant" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = SlateDark
                        )
                    }
                }

                // Action Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isNativeApp) HydrogenPillBg else Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionBadgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNativeApp) HydrogenBlue else SlateMuted
                    )
                }
            }

            // Natural Speech / Action Feedback (Clear, clean, readable)
            if (intent.speechFeedback.isNotBlank()) {
                Text(
                    text = intent.speechFeedback,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateDark,
                    lineHeight = 20.sp
                )
            }

            // Clean WhatsApp Message Preview (Only if real message parameters exist)
            val contact = intent.details["contact"] ?: ""
            val message = intent.details["message"] ?: (if (intent.action == ActionType.SEND_WHATSAPP) intent.query else "")
            if (intent.action == ActionType.SEND_WHATSAPP && message.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0FDF4))
                        .border(1.dp, Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (contact.isNotBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "To:", fontSize = 11.sp, color = SlateSecondary)
                                Text(
                                    text = contact.replaceFirstChar { it.uppercase() },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "Message:", fontSize = 11.sp, color = SlateSecondary)
                            Text(
                                text = "\"$message\"",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            }

            // Multi-Store Price Comparison Chips (Only if user genuinely asked for price comparison)
            val plan = intent.autonomousPlan
            if (plan != null && plan.priceComparisons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🛒 Store Comparison:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        plan.priceComparisons.forEach { item ->
                            val isBest = item.badge.contains("Best", ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isBest) Color(0xFFECFDF5) else Color(0xFFF8FAFC))
                                    .border(
                                        1.dp,
                                        if (isBest) Color(0xFF10B981) else Color(0xFFE2E8F0),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onStoreClick?.invoke(item.store, item.query)
                                    }
                                    .padding(8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = item.store,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isBest) Color(0xFF065F46) else SlateDark
                                    )
                                    Text(
                                        text = item.price,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isBest) Color(0xFF047857) else Color(0xFF1E293B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Clean Minimal Status Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isNativeApp) Color(0xFF10B981) else Color(0xFF94A3B8))
                    )
                    Text(
                        text = if (isNativeApp) "Native App Active" else "Assistant Ready",
                        fontSize = 11.sp,
                        color = SlateMuted
                    )
                }

                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        color = SlateSecondary
                    )
                }
            }
        }
    }
}
