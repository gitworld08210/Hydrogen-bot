package com.hydrogen.bot.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrogen.bot.R
import com.hydrogen.bot.auth.AuthManager
import com.hydrogen.bot.ui.theme.CanvasWhite
import com.hydrogen.bot.ui.theme.HairlineBorder
import com.hydrogen.bot.ui.theme.SlateDark
import com.hydrogen.bot.ui.theme.SlateMuted
import com.hydrogen.bot.ui.theme.SlateSecondary
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authManager: AuthManager,
    onLoginSuccess: () -> Unit,
    onBiometricLogin: () -> Unit = {},
    hasSavedSession: Boolean = false,
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
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // TOP EDITORIAL BRAND HEADER (From reference design)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "HYDROGEN",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 4.sp,
                    color = SlateDark
                )
                Text(
                    text = "+",
                    fontSize = 14.sp,
                    color = SlateMuted
                )
            }

            // CENTER HERO ILLUSTRATION / BADGE — Hydrogen mascot
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, HairlineBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.hydrogen_mascot),
                    contentDescription = "Hydrogen mascot",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // BOTTOM EDITORIAL COPY & INPUT SECTION
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Editorial Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "AI",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = SlateDark
                    )
                    Text(
                        text = "• Action Created Instantly",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateSecondary
                    )
                }

                Text(
                    text = "That Understands Your World.",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    color = SlateDark,
                    lineHeight = 26.sp
                )

                // Input fields
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    enabled = !isOtpSent && !isLoading,
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = SlateMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF111215),
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = CanvasWhite
                    ),
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isOtpSent) {
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { if (it.length <= 6) otp = it },
                        label = { Text("6-Digit OTP Code") },
                        singleLine = true,
                        enabled = !isLoading,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = SlateMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF111215),
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = CanvasWhite
                        ),
                        shape = RoundedCornerShape(20.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Action Pill Button (Dark Obsidian Pill from reference)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111215)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = CanvasWhite, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (!isOtpSent) "Send OTP" else "Verify & Continue",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                if (hasSavedSession && !isOtpSent) {
                    OutlinedButton(
                        onClick = onBiometricLogin,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = SlateDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unlock with Fingerprint / Face",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateDark
                        )
                    }
                }

                statusMessage?.let {
                    Text(
                        text = it,
                        fontSize = 12.sp,
                        color = SlateDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isOtpSent) {
                    TextButton(
                        onClick = {
                            isOtpSent = false
                            otp = ""
                            statusMessage = null
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Change Email Address", color = SlateSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
