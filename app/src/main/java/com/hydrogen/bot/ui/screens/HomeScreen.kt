package com.hydrogen.bot.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.hydrogen.bot.ai.VisionResult
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
    onOpenVision: () -> Unit = {},
    visionResult: VisionResult? = null,
    onClearVision: () -> Unit = {},
    onSearchAmazon: (String) -> Unit = {},
    onSearchFlipkart: (String) -> Unit = {},
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

            // Right Action Icons (Camera Vision + Vault Lock)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera Vision Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, HairlineBorder, CircleShape)
                        .clickable { onOpenVision() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Camera Vision",
                        tint = SlateDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Circular Vault Button
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

                // The 4 Organic Blurred Sound Capsules
                OrganicSoundCapsules(
                    isListening = isListening,
                    volumeRms = volumeRms,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                // Capsule Pill Badge
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

                Spacer(modifier = Modifier.height(16.dp))

                // Main Title Headline
                Text(
                    text = "Smart Voice Assistant",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle / Assistant State
                Text(
                    text = if (isListening) "Listening in Hindi & English…" else "I'm Here To Assist You With Anything Today",
                    fontSize = 14.sp,
                    color = SlateSecondary,
                    textAlign = TextAlign.Center
                )

                // Voice Transcript Box (If spoken)
                if (transcriptText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
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

                // Vision Result Card (If photo captured)
                visionResult?.let { vision ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, HairlineBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = null,
                                        tint = SlateDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Hydrogen Vision Lens",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateDark
                                    )
                                }
                                IconButton(
                                    onClick = onClearVision,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = SlateMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Text(
                                text = vision.productName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SlateDark
                            )

                            Text(
                                text = vision.description,
                                fontSize = 13.sp,
                                color = SlateSecondary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (vision.amazonQuery.isNotBlank()) {
                                    Button(
                                        onClick = { onSearchAmazon(vision.amazonQuery) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111215)),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text("Amazon", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                                if (vision.flipkartQuery.isNotBlank()) {
                                    Button(
                                        onClick = { onSearchFlipkart(vision.flipkartQuery) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2874F0)),
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                    ) {
                                        Text("Flipkart", color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
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
                    // Left Mode Button (Chat / AI Ask)
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
                            contentDescription = "Text Chat",
                            tint = SlateDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Center Hero Obsidian Mic Button with Diffused Ring
                    HydrogenMicButton(
                        isListening = isListening,
                        onClick = onMicClick
                    )

                    // Right Camera Quick Action
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CanvasWhite)
                            .border(1.dp, HairlineBorder, CircleShape)
                            .clickable { onOpenVision() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Camera Vision",
                            tint = SlateDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            // ==================== MODE 2: AI ASK / PRODUCT CURATION (SCREEN 3 IN REFERENCE) ====================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Editorial Title
                Text(
                    text = "AI Ask",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SlateDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mood / Category Pills (Scrollable Row)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moodCategories) { cat ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (cat == "Shoes") Color(0xFF111215) else Color(0xFFF8FAFC))
                                .border(1.dp, if (cat == "Shoes") Color(0xFF111215) else HairlineBorder, RoundedCornerShape(20.dp))
                                .clickable { onQuickCommandClick("Amazon pe $cat search karo") }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (cat == "Shoes") Color.White else SlateDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Curated 2-Column Product Cards (From reference)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    curatedProducts.forEach { product ->
                        ProductCard(
                            product = product,
                            onClick = {
                                onQuickCommandClick("Amazon pe ${product.title} search karo")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Assistant Voice Chips
                Text(
                    text = "Quick Assistant Actions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateDark
                )
                Spacer(modifier = Modifier.height(10.dp))

                val quickActions = listOf(
                    "🌅 Good Morning Briefing",
                    "📸 Photo Scan Product",
                    "⚡ Check Phone Battery",
                    "👟 Shoes on Amazon",
                    "🎵 Arijit Singh Songs",
                    "💬 WhatsApp Message"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickActions.forEach { action ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    val query = when {
                                        action.contains("Good Morning") -> "Good morning"
                                        action.contains("Photo Scan") -> { onOpenVision(); return@clickable }
                                        action.contains("Battery") -> "battery check"
                                        action.contains("Shoes") -> "Amazon pe running shoes"
                                        action.contains("Songs") -> "YouTube pe Arijit Singh gaane"
                                        action.contains("WhatsApp") -> "WhatsApp pe message"
                                        else -> action
                                    }
                                    onQuickCommandClick(query)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = action,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = SlateDark
                            )
                        }
                    }
                }
            }

            // Bottom Text Query Input Bar (For Typing Mode)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask Hydrogen anything…", color = SlateMuted, fontSize = 14.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF111215),
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = CanvasWhite
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF111215))
                        .clickable {
                            if (textInput.isNotBlank()) {
                                onQuickCommandClick(textInput)
                                textInput = ""
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
