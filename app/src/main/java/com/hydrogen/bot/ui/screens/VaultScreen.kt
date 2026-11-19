package com.hydrogen.bot.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hydrogen.bot.security.EncryptedVaultManager
import com.hydrogen.bot.security.VaultItem
import com.hydrogen.bot.ui.theme.CanvasWhite
import com.hydrogen.bot.ui.theme.HairlineBorder
import com.hydrogen.bot.ui.theme.HydrogenBlue
import com.hydrogen.bot.ui.theme.SlateDark
import com.hydrogen.bot.ui.theme.SlateMuted
import com.hydrogen.bot.ui.theme.SlateSecondary
import com.hydrogen.bot.ui.theme.SurfacePearl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    vaultManager: EncryptedVaultManager,
    onBack: () -> Unit,
    onAuthenticateBiometric: (onSuccess: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var items by remember { mutableStateOf(vaultManager.getAllItems()) }
    var unlockedItemId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biometric Secure Vault", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SlateDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SlateDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CanvasWhite)
            )
        },
        containerColor = CanvasWhite
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Security badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfacePearl)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = HydrogenBlue, modifier = Modifier.size(22.dp))
                Text(
                    text = "Hardware AES-256 KeyStore protected. Fingerprint scan required to view passwords.",
                    fontSize = 12.sp,
                    color = SlateSecondary
                )
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Koi password ya details save nahi hain.\nAap voice se bhi add kar sakte hain.",
                        fontSize = 14.sp,
                        color = SlateMuted
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items) { item ->
                        val isRevealed = unlockedItemId == item.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CanvasWhite),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = SlateDark)
                                    Text(item.usernameOrEmail, fontSize = 13.sp, color = SlateSecondary)
                                    Text(
                                        text = if (isRevealed) item.secretValue else "••••••••••••",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isRevealed) HydrogenBlue else SlateMuted
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isRevealed) {
                                            unlockedItemId = null
                                        } else {
                                            onAuthenticateBiometric {
                                                unlockedItemId = item.id
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Reveal",
                                        tint = HydrogenBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
