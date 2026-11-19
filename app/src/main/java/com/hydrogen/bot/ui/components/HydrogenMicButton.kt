package com.hydrogen.bot.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Redesigned to exactly match the reference design:
 * Dark obsidian circle, crisp white icon, soft diffused outer halo.
 */
@Composable
fun HydrogenMicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "micHalo")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(100.dp)
    ) {
        // Outer diffused soft halo ring (from reference image)
        Box(
            modifier = Modifier
                .size(86.dp)
                .scale(if (isListening) haloScale else 1f)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0).copy(alpha = if (isListening) 0.6f else 0.4f))
                .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.5f), CircleShape)
        )

        // Center Deep Obsidian Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .shadow(
                    elevation = if (isListening) 12.dp else 6.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.35f)
                )
                .clip(CircleShape)
                .background(Color(0xFF111215)) // Pure obsidian black
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = "Voice Input",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
