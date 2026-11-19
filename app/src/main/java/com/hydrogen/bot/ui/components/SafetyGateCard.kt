package com.hydrogen.bot.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrogen.bot.ui.theme.CanvasWhite
import com.hydrogen.bot.ui.theme.SafetyAmber
import com.hydrogen.bot.ui.theme.SlateDark
import com.hydrogen.bot.ui.theme.SlateSecondary

@Composable
fun SafetyGateCard(
    reason: String = "Payment Screen Reached — Bot action stopped for security.",
    onUserAuthorize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, SafetyAmber.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CanvasWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = SafetyAmber,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "SAFETY GUARDRAIL ACTIVE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = SafetyAmber
                )
            }

            Text(
                text = reason,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = SlateDark
            )

            Text(
                text = "Hydrogen Bot paise deduct nahi karega. Kripya apna UPI PIN ya OTP enter karke payment khud complete karein.",
                fontSize = 13.sp,
                color = SlateSecondary,
                lineHeight = 18.sp
            )

            Button(
                onClick = onUserAuthorize,
                colors = ButtonDefaults.buttonColors(containerColor = SafetyAmber),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "I Understand & Proceed Safely",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
