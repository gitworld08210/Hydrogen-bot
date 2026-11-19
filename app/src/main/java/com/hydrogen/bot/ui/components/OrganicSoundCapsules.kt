package com.hydrogen.bot.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The signature 4-capsule organic soundwave from the reference design.
 * Features dark obsidian pills with soft blurred edges and volume reactivity.
 */
@Composable
fun OrganicSoundCapsules(
    isListening: Boolean,
    volumeRms: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "capsuleIdle")
    val idleFloat by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idleScale"
    )

    // Base heights for the 4 capsules (from reference image: small, tall, medium-tall, medium)
    val baseHeights = listOf(48.dp, 84.dp, 64.dp, 52.dp)
    val widths = listOf(30.dp, 34.dp, 34.dp, 30.dp)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 4) {
            val targetHeight = if (isListening) {
                val factor = (i % 2 + 1) * 0.4f
                val dynamic = (baseHeights[i].value * (1f + (volumeRms * factor * 0.2f))).coerceIn(36f, 100f)
                dynamic.dp
            } else {
                (baseHeights[i].value * idleFloat).dp
            }

            val animatedHeight by animateDpAsState(
                targetValue = targetHeight,
                animationSpec = spring(dampingRatio = 0.7f),
                label = "capsule$i"
            )

            Box(
                modifier = Modifier
                    .width(widths[i])
                    .height(animatedHeight)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(100.dp),
                        spotColor = Color.Black.copy(alpha = 0.35f),
                        ambientColor = Color.Black.copy(alpha = 0.15f)
                    )
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFF111215)) // Deep obsidian black
            )
        }
    }
}
