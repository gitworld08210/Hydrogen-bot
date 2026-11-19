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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrogen.bot.ai.ParsedIntent
import com.hydrogen.bot.ui.components.ActionCard
import com.hydrogen.bot.ui.components.HydrogenMicButton
import com.hydrogen.bot.ui.components.OrganicSoundCapsules
import com.hydrogen.bot.ui.components.ProductCard
import com.hydrogen.bot.ui.components.ProductItem
import com.hydrogen.bot.ui.components.SafetyGateCard
import com.hydrogen.bot.ui.theme.CanvasWhite
import com.hydrogen.bot.ui.theme.HairlineBorder
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
    // Mode toggle between "Voice Chat" (Middle Screen) and "AI Ask" (Right Screen)
    var isChatView by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    val moodCategories = listOf("Shoes", "Electronics", "Mobiles", "Watches", "Deals")
    val curatedProducts = listOf(
        ProductItem("1", "Running Shoes Elite", "₹1,499.00", "15% Off", "Amazon"),
        ProductItem("2", "Wireless Earbuds Pro", "₹999.00", "20% Off", "Amazon")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP APP BAR (From reference design)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Back / Mode Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, HairlineBorder, CircleShape)
                    .clickable { isChatView = !isChatView },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Toggle",
                    tint = SlateDark,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Center Title
            Text(
                text = if (isChatView) "AI Ask" else "Voice Chat",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = SlateDark
            )

            // Right Circular Vault / Bag Button
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, HairlineBorder, CircleShape)
                    .clickable { onOpenVault() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault",
                    tint = SlateDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // MIDDLE CONTENT: Toggle between Voice Mode & Chat/Product Mode
        if (!isChatView) {
            // ==================== MODE 1: VOICE CHAT (SCREEN 2 IN REFERENCE) ====================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // The Signature 4 Organic Sound Capsules
                OrganicSoundCapsules(
                    isListening = isListening,
                    volumeRms = volumeRms
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Pill Tag Badge: "Your Assistant 3.0"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Hydrogen Assistant 3.0",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Headline
                Text(
                    text = "Smart Voice Assistant",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = if (isListening) "Listening in Hindi & English…" else "I'm Here To Assist You With Anything Today",
                    fontSize = 14.sp,
                    color = SlateSecondary,
                    textAlign = TextAlign.Center
                )

                // Voice Transcript Box (If spoken)
                if (transcriptText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF8FAFC))
                            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "\"$transcriptText\"",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = SlateDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Action or Safety Card
                lastIntent?.let { intent ->
                    Spacer(modifier = Modifier.height(16.dp))
                    if (intent.requiresSafetyPause) {
                        SafetyGateCard(
                            reason = intent.speechFeedback,
                            onUserAuthorize = {}
                        )
                    } else {
                        ActionCard(
                            intent = intent,
                            isNativeApp = isLastNativeApp,
                            statusText = intent.speechFeedback
                        )
                    }
                }
            }

            // BOTTOM DOCK (From reference design)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .padding(horizontal = 24.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Chat Toggle Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CanvasWhite)
                            .border(1.dp, HairlineBorder, CircleShape)
                            .clickable { isChatView = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Chat",
                            tint = SlateDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center: Black Elevated Hydrogen Mic Button
                    HydrogenMicButton(
                        isListening = isListening,
                        onClick = onMicClick
                    )

                    // Right: Close Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CanvasWhite)
                            .border(1.dp, HairlineBorder, CircleShape)
                            .clickable {
                                // Reset view or cancel
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = SlateDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // ==================== MODE 2: AI ASK / PRODUCT CURATION (SCREEN 3) ====================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Bot Greeting Bubble
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "What's Your Shopping Mood Today?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateDark
                    )
                }

                // Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(moodCategories) { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                                .clickable { onQuickCommandClick("Amazon pe $cat search karo") }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 13.sp,
                                color = SlateDark
                            )
                        }
                    }
                }

                // User Request Bubble (If query available)
                if (transcriptText.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF111215))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = transcriptText,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // AI Curated Products Header
                Text(
                    text = "AI-Curated Finds Based On Your Mood",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateMuted
                )

                // 2-Column Product Cards Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    curatedProducts.forEach { product ->
                        Box(modifier = Modifier.weight(1f)) {
                            ProductCard(
                                product = product,
                                onClick = { onQuickCommandClick("Amazon pe ${product.title} search karo") }
                            )
                        }
                    }
                }

                lastIntent?.let { intent ->
                    ActionCard(
                        intent = intent,
                        isNativeApp = isLastNativeApp,
                        statusText = intent.speechFeedback
                    )
                }
            }

            // BOTTOM PILL INPUT BAR (Screen 3 Bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, HairlineBorder, RoundedCornerShape(28.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera",
                        tint = SlateMuted,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice",
                        tint = SlateMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onMicClick() }
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Type here…", fontSize = 14.sp, color = SlateMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onQuickCommandClick(textInput)
                                textInput = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color(0xFF111215),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
