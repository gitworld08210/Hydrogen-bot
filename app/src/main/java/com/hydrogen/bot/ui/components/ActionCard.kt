package com.hydrogen.bot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
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
import androidx.compose.ui.graphics.vector.ImageVector
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
    modifier: Modifier = Modifier
) {
    val (appColor, appIcon) = when (intent.action) {
        ActionType.SEARCH_AMAZON -> Pair(BrandAmazon, Icons.Default.ShoppingBag)
        ActionType.SEARCH_YOUTUBE -> Pair(BrandYouTube, Icons.Default.SmartDisplay)
        ActionType.SEND_WHATSAPP -> Pair(BrandWhatsApp, Icons.Default.CheckCircle)
        else -> Pair(HydrogenBlue, Icons.Default.OpenInNew)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CanvasWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: App Badge & Native Indicator
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
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(appColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = appIcon,
                            contentDescription = null,
                            tint = appColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = intent.targetApp.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SlateDark
                    )
                }

                // Native App Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isNativeApp) HydrogenPillBg else HairlineBorder.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isNativeApp) "NATIVE APP" else "FALLBACK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isNativeApp) HydrogenBlue else SlateMuted
                    )
                }
            }

            // Query Details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = intent.query,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateDark
                )
                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = if (isNativeApp) SuccessGreen else SlateSecondary
                )
            }
        }
    }
}
