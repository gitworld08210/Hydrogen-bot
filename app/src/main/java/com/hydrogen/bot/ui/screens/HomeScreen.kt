package com.hydrogen.bot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrogen.bot.ai.ParsedIntent
import com.hydrogen.bot.ui.components.ActionCard
import com.hydrogen.bot.ui.components.HydrogenMicButton
import com.hydrogen.bot.ui.components.SafetyGateCard
import com.hydrogen.bot.ui.components.WaveformVisualizer
import com.hydrogen.bot.ui.theme.CanvasWhite
import com.hydrogen.bot.ui.theme.HairlineBorder
import com.hydrogen.bot.ui.theme.HydrogenBlue
import com.hydrogen.bot.ui.theme.HydrogenPillBg
import com.hydrogen.bot.ui.theme.SlateDark
import com.hydrogen.bot.ui.theme.SlateMuted
import com.hydrogen.bot.ui.theme.SlateSecondary
import com.hydrogen.bot.ui.theme.SurfacePearl

@Composable
fun HomeScreen(
    isListening: Boolean,
    volumeRms: Float,
    statusText: String,
    transcriptText: String,
    lastIntent: ParsedIntent?,
    isLastNativeApp: Boolean,
    userEmail: String,
    onMicClick: () -> Unit,
    onOpenVault: () -> Unit,
    onQuickCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val quickSuggestions = listOf(
        "Amazon pe running shoes search karo",
        "YouTube pe Arijit Singh ke gaane chalao",
        "Flipkart pe wireless earbuds dhundo",
        "WhatsApp pe message bhejo"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hydrogen Bot",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark
                )
                Text(
                    text = "Lightweight Autonomous Voice Assistant",
                    fontSize = 12.sp,
                    color = SlateMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Secure Vault Shortcut Button
                IconButton(
                    onClick = onOpenVault,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfacePearl)
                        .border(1.dp, HairlineBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Password Vault",
                        tint = HydrogenBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // User Email Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfacePearl)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = SlateSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = userEmail.substringBefore("@"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateDark
                        )
                    }
                }
            }
        }

        // MIDDLE CONTENT: Live Status, Transcript & Action Cards
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // State & Waveform Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(SurfacePearl)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WaveformVisualizer(
                        isListening = isListening,
                        volumeRms = volumeRms
                    )
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isListening) HydrogenBlue else SlateDark
                    )
                }
            }

            // Real-Time Transcript Display
            if (transcriptText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfacePearl)
                        .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "VOICE INPUT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SlateMuted
                        )
                        Text(
                            text = "\"$transcriptText\"",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = SlateDark
                        )
                    }
                }
            }

            // Action Result Card
            lastIntent?.let { intent ->
                if (intent.requiresSafetyPause) {
                    SafetyGateCard(
                        reason = intent.speechFeedback,
                        onUserAuthorize = { /* Handled safely */ }
                    )
                } else {
                    ActionCard(
                        intent = intent,
                        isNativeApp = isLastNativeApp,
                        statusText = intent.speechFeedback
                    )
                }
            }

            // Quick Example Command Chips
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "TRY ASKING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateMuted
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickSuggestions) { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfacePearl)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                                .clickable { onQuickCommandClick(suggestion) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 12.sp,
                                color = SlateSecondary
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM HERO: Minimalist White Hydrogen Mic Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            HydrogenMicButton(
                isListening = isListening,
                onClick = onMicClick
            )

            Text(
                text = if (isListening) "Listening in Hindi & English…" else "Tap button to speak",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = if (isListening) HydrogenBlue else SlateMuted
            )
        }
    }
}
