package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DexPaidStatus
import com.example.data.model.PumpToken
import com.example.data.model.RiskLevel
import com.example.data.model.TokenLaunchStage
import com.example.ui.theme.*

@Composable
fun TokenCard(
    token: PumpToken,
    onClick: () -> Unit,
    onQuickBuy: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Flash background slightly on price update
    val isSpike = token.change1m > 0
    val priceChangeColor by animateColorAsState(
        targetValue = if (isSpike) NeonGreen else DangerRed,
        animationSpec = tween(durationMillis = 400),
        label = "priceColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("token_card_${token.symbol}")
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (token.dexPaidStatus == DexPaidStatus.CONFIRMED) DexPaidGold.copy(alpha = 0.5f) else CardBorder
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Avatar + Name/Symbol + Badges + Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Meme Emoji Avatar Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(token.avatarBgColor), RoundedCornerShape(12.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = token.avatarEmoji,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Name & Symbol & Address
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = token.symbol,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (token.stage == TokenLaunchStage.GRADUATED) {
                            Box(
                                modifier = Modifier
                                    .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("🎓 Raydium", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text(
                        text = token.name,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Dex Paid Badge
                if (token.dexPaidStatus == DexPaidStatus.CONFIRMED) {
                    Box(
                        modifier = Modifier
                            .background(DexPaidGoldBg, RoundedCornerShape(8.dp))
                            .border(1.dp, DexPaidGold, RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("⚡", fontSize = 10.sp)
                            Text(
                                text = "DEX PAID",
                                color = DexPaidGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                // Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (token.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (token.isFavorite) DexPaidGold else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Row: Price, 1m change, Market Cap, Rug Safety Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Price & Change
                Column {
                    Text(
                        text = "${String.format("%.7f", token.priceSol)} SOL",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$${String.format("%.6f", token.priceUsd)}",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                        val changeSign = if (token.change1m >= 0) "+" else ""
                        Text(
                            text = "$changeSign${String.format("%.1f", token.change1m)}%",
                            color = priceChangeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Market Cap & Volume
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "MCap: ${token.formattedMarketCap}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Vol: ${token.formattedVolume}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bonding Curve Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Bonding Curve",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${(token.bondingCurveProgress * 100).toInt()}% to Raydium",
                        color = if (token.bondingCurveProgress > 0.8f) NeonGreen else TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                LinearProgressIndicator(
                    progress = { token.bondingCurveProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = if (token.bondingCurveProgress > 0.8f) NeonGreen else NeonCyan,
                    trackColor = CardHighlight,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions & Socials
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Social Links row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Telegram
                    if (token.telegramUrl.isNotBlank()) {
                        SocialIconButton(
                            label = "TG",
                            icon = Icons.Default.Send,
                            onClick = {
                                openUrl(context, token.telegramUrl)
                            }
                        )
                    }
                    // Twitter / X
                    if (token.twitterUrl.isNotBlank()) {
                        SocialIconButton(
                            label = "X",
                            icon = Icons.Default.AlternateEmail,
                            onClick = {
                                openUrl(context, token.twitterUrl)
                            }
                        )
                    }
                    // Website
                    if (token.websiteUrl.isNotBlank()) {
                        SocialIconButton(
                            label = "Web",
                            icon = Icons.Default.Language,
                            onClick = {
                                openUrl(context, token.websiteUrl)
                            }
                        )
                    }
                    // Copy Address
                    SocialIconButton(
                        label = token.shortAddress,
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            copyToClipboard(context, token.id)
                        }
                    )
                }

                // Rug Score badge + Quick Snipe Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rug Score Pill
                    val rugBadgeColor = when (token.rugAnalysis.riskLevel) {
                        RiskLevel.SAFE -> SafeGreen
                        RiskLevel.MODERATE -> WarningAmber
                        RiskLevel.HIGH_RISK -> DangerRed
                    }
                    Box(
                        modifier = Modifier
                            .background(rugBadgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, rugBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🛡️ ${token.rugAnalysis.safetyScore}",
                            color = rugBadgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Snipe Button
                    Button(
                        onClick = onQuickBuy,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("quick_snipe_${token.symbol}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = CyberBlack
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Snipe", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialIconButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(CardHighlight, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = NeonCyan,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Opening: $url", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Pump Mint Address", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Contract copied!", Toast.LENGTH_SHORT).show()
}
