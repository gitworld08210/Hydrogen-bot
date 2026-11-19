package com.hydrogen.bot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrogen.bot.auth.AuthManager
import com.hydrogen.bot.ui.theme.CanvasWhite
import com.hydrogen.bot.ui.theme.HairlineBorder
import com.hydrogen.bot.ui.theme.HydrogenBlue
import com.hydrogen.bot.ui.theme.SlateDark
import com.hydrogen.bot.ui.theme.SlateMuted
import com.hydrogen.bot.ui.theme.SlateSecondary
import com.hydrogen.bot.ui.theme.SurfacePearl
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasWhite)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Brand Header
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(HydrogenBlue.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = HydrogenBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "Welcome to Hydrogen Bot",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = SlateDark
            )

            Text(
                text = if (!isOtpSent) "Apna email enter karein, hum login ke liye 6-digit OTP bhejenge."
                else "6-digit OTP '$email' par bheja gaya hai.",
                fontSize = 14.sp,
                color = SlateSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Email Input Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                singleLine = true,
                enabled = !isOtpSent && !isLoading,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SlateMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HydrogenBlue,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = SurfacePearl,
                    unfocusedContainerColor = CanvasWhite
                ),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            // OTP Input Field (Shows after OTP is dispatched)
            if (isOtpSent) {
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    label = { Text("6-Digit OTP Code") },
                    singleLine = true,
                    enabled = !isLoading,
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SlateMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HydrogenBlue,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = SurfacePearl,
                        unfocusedContainerColor = CanvasWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action Button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        statusMessage = null
                        if (!isOtpSent) {
                            val res = authManager.requestEmailOtp(email)
                            res.onSuccess {
                                isOtpSent = true
                                statusMessage = it
                            }.onFailure {
                                statusMessage = it.localizedMessage
                            }
                        } else {
                            val res = authManager.verifyEmailOtp(email, otp)
                            res.onSuccess {
                                onLoginSuccess()
                            }.onFailure {
                                statusMessage = it.localizedMessage
                            }
                        }
                        isLoading = false
                    }
                },
                enabled = if (!isOtpSent) email.isNotBlank() && !isLoading else otp.length == 6 && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = HydrogenBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = CanvasWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (!isOtpSent) "Send Email OTP" else "Verify & Continue",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Status message
            statusMessage?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = if (it.contains("error", ignoreCase = true) || it.contains("Galat", ignoreCase = true)) SlateDark else HydrogenBlue,
                    textAlign = TextAlign.Center
                )
            }

            if (isOtpSent) {
                TextButton(
                    onClick = {
                        isOtpSent = false
                        otp = ""
                        statusMessage = null
                    }
                ) {
                    Text("Change Email Address", color = SlateSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}
