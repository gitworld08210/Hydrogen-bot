package com.hydrogen.bot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartDisplay
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
import com.hydrogen.bot.ui.theme.BrandAmazon
import com.hydrogen.bot.ui.theme.BrandWhatsApp
import com.hydrogen.bot.ui.theme.BrandYouTube
import com.hydrogen.bot.ui.theme.CanvasWhite
import com.hydrogen.bot.ui.theme.HairlineBorder
import com.hydrogen.bot.ui.theme.HydrogenBlue
import com.hydrogen.bot.ui.theme.HydrogenPillBg
import com.hydrogen.bot.ui.theme.SlateDark
import com.hydrogen.bot.ui.theme.SlateMuted
import com.hydrogen.bot.ui.theme.SlateSecondary
import com.hydrogen.bot.ui.theme.SuccessGreen

@Composable
fun ActionCard(
    intent: ParsedIntent,
    isNativeApp: Boolean,
    statusText: String,
    onStoreClick: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val plan = intent.autonomousPlan
    val (appColor, appIcon) = when (intent.action) {
        ActionType.SEARCH_AMAZON -> Pair(BrandAmazon, Icons.Default.ShoppingBag)
        ActionType.SEARCH_YOUTUBE -> Pair(BrandYouTube, Icons.Default.SmartDisplay)
        ActionType.SEND_WHATSAPP -> Pair(BrandWhatsApp, Icons.Default.CheckCircle)
        else -> Pair(HydrogenBlue, Icons.AutoMirrored.Filled.OpenInNew)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CanvasWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Hermes Autonomous Cloud Brain / App Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (plan != null) Color(0xFF1E293B) else appColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (plan != null) Icons.Default.Psychology else appIcon,
                            contentDescription = null,
                            tint = if (plan != null) Color(0xFF38BDF8) else appColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (plan != null) "HERMES CLOUD BRAIN" else intent.targetApp.uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (plan != null) Color(0xFF0284C7) else SlateDark
                        )
                        if (plan != null) {
                            Text(
                                text = "Autonomous Web Scrape & Reasoning",
                                fontSize = 10.sp,
                                color = SlateMuted
                            )
                        }
                    }
                }

                // Native App or Cloud Active Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNativeApp) HydrogenPillBg else Color(0xFFF1F5F9))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (plan != null) "LIVE CLOUD" else (if (isNativeApp) "NATIVE APP" else (if (intent.action == ActionType.GENERAL_QUERY) "CHAT" else "FALLBACK")),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (plan != null) Color(0xFF0369A1) else (if (isNativeApp) HydrogenBlue else SlateMuted)
                    )
                }
            }

            // Cloud Reasoning Summary
            if (plan != null && plan.cloudReasoning.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "🧠 Hermes Live Analysis:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = plan.cloudReasoning,
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Multi-Store Price Comparison Chips (Amazon vs Flipkart vs Blinkit)
            if (plan != null && plan.priceComparisons.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "🛒 Multi-Store Price Comparison:",
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
                                    if (item.badge.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isBest) Color(0xFFD1FAE5) else Color(0xFFE2E8F0))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.badge,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isBest) Color(0xFF065F46) else SlateSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Step-by-Step Instructions ("Kya kya karna hai")
            if (plan != null && plan.steps.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📋 Autonomous Execution Steps:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SlateDark
                    )
                    plan.steps.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (step.isDone) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (step.isDone) "✓" else "${step.stepIndex}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (step.isDone) Color(0xFF16A34A) else SlateMuted
                                )
                            }
                            Column {
                                Text(
                                    text = step.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SlateDark
                                )
                                Text(
                                    text = step.detail,
                                    fontSize = 10.sp,
                                    color = SlateSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Auto-Type Query Indicator ("Kya type karna hai")
            if (plan != null && !plan.autoTypeText.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("⌨️", fontSize = 12.sp)
                    Text(
                        text = "Auto-Typed: \"${plan.autoTypeText}\"",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF92400E)
                    )
                }
            }

            // Supabase Long-Term Memory Sync Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = "Supabase",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Supabase Memory Active",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF047857)
                    )
                }

                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    color = if (isNativeApp) SuccessGreen else SlateSecondary
                )
            }
        }
    }
}
