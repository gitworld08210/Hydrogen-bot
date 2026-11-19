package com.hydrogen.bot.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.unit.dp
import com.hydrogen.bot.ui.theme.HydrogenBlue

@Composable
fun WaveformVisualizer(
    isListening: Boolean,
    volumeRms: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 5
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val targetHeight = if (isListening) {
                // Modulate height based on RMS volume (between 6dp and 28dp)
                val factor = (i % 3 + 1) * 0.35f
                val h = (volumeRms * factor * 5f).coerceIn(6f, 28f)
                h.dp
            } else {
                4.dp
            }

            val animatedHeight by animateFloatAsState(
                targetValue = targetHeight,
                animationSpec = spring(),
                label = "waveBar$i"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(HydrogenBlue.copy(alpha = if (isListening) 0.85f else 0.25f))
            )
        }
    }
}
