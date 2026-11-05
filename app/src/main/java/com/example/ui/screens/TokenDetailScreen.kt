package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DexPaidStatus
import com.example.data.model.PortfolioHolding
import com.example.data.model.PumpToken
import com.example.data.model.TokenLaunchStage
import com.example.ui.components.RugCheckCard
import com.example.ui.components.TechnicalChart
import com.example.ui.theme.*

@Composable
fun TokenDetailScreen(
    token: PumpToken,
    walletSol: Double,
    holding: PortfolioHolding?,
    onBack: () -> Unit,
    onOpenTrade: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack,
        bottomBar = {
            // Persistent Bottom Trading Action Bar
            Surface(
                color = DarkNavy,
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (holding != null && holding.amountTokens > 0) {
                        Button(
                            onClick = onOpenTrade,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Sell Holding", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = onOpenTrade,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("detail_trade_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen,
                            contentColor = CyberBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Trade / Limit Order", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header: Back Button, Title, Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(CardHighlight, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(token.avatarBgColor), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(token.avatarEmoji, fontSize = 20.sp)
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = token.symbol,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (token.dexPaidStatus == DexPaidStatus.CONFIRMED) {
                                Box(
                                    modifier = Modifier
                                        .background(DexPaidGoldBg, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("⚡ DEX PAID", color = DexPaidGold, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        Text(
                            text = token.name,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (token.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (token.isFavorite) DexPaidGold else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Price Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Price", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = "${String.format("%.7f", token.priceSol)} SOL",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "$${String.format("%.6f", token.priceUsd)}",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 1m & 5m changes
                        Column(horizontalAlignment = Alignment.End) {
                            val changeSign = if (token.change5m >= 0) "+" else ""
                            val changeColor = if (token.change5m >= 0) NeonGreen else DangerRed
                            Box(
                                modifier = Modifier
                                    .background(changeColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$changeSign${String.format("%.2f", token.change5m)}% (5m)",
                                    color = changeColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "5m: ${if (token.change5m >= 0) "+" else ""}${String.format("%.1f", token.change5m)}%",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Grid (MCap, Volume, Holders, Bonding Curve)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricBox(label = "Market Cap", value = token.formattedMarketCap, modifier = Modifier.weight(1f))
                        MetricBox(label = "24h Volume", value = token.formattedVolume, modifier = Modifier.weight(1f))
                        MetricBox(label = "Holders", value = "${token.holderCount}", modifier = Modifier.weight(1f))
                        MetricBox(
                            label = "Bonding",
                            value = "${(token.bondingCurveProgress * 100).toInt()}%",
                            highlightColor = if (token.bondingCurveProgress > 0.8f) NeonGreen else TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // External Links & Contract Address Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Official Links:", color = TextMuted, fontSize = 11.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (token.telegramUrl.isNotBlank()) {
                            SocialButton("Telegram", Icons.Default.Send) {
                                openUrl(context, token.telegramUrl)
                            }
                        }
                        if (token.twitterUrl.isNotBlank()) {
                            SocialButton("X / Twitter", Icons.Default.AlternateEmail) {
                                openUrl(context, token.twitterUrl)
                            }
                        }
                        if (token.websiteUrl.isNotBlank()) {
                            SocialButton("Website", Icons.Default.Language) {
                                openUrl(context, token.websiteUrl)
                            }
                        }
                        SocialButton("Pump.fun", Icons.Default.OpenInNew) {
                            openUrl(context, "https://pump.fun/coin/${token.id}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Technical Chart
            Text(
                text = "Technical Price Action",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            TechnicalChart(
                candles = token.candles,
                currentPriceSol = token.priceSol
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Rug Pull Detector Inspector
            Text(
                text = "Rug Pull & Smart Contract Security",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            RugCheckCard(rugAnalysis = token.rugAnalysis)

            Spacer(modifier = Modifier.height(16.dp))

            // User's current holding for this token
            if (holding != null && holding.amountTokens > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(NeonGreen.copy(alpha = 0.5f)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Your Holding Status", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tokens Owned", color = TextMuted, fontSize = 10.sp)
                                Text("${String.format("%,.0f", holding.amountTokens)} ${token.symbol}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Current Value", color = TextMuted, fontSize = 10.sp)
                                Text("${String.format("%.4f", holding.currentValSol(token.priceSol))} SOL", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Unrealized PnL", color = TextMuted, fontSize = 10.sp)
                                val pnlSol = holding.pnlSol(token.priceSol)
                                val pnlPct = holding.pnlPct(token.priceSol)
                                val pnlColor = if (pnlSol >= 0) NeonGreen else DangerRed
                                Text(
                                    text = "${if (pnlSol >= 0) "+" else ""}${String.format("%.4f", pnlSol)} SOL (${String.format("%.1f", pnlPct)}%)",
                                    color = pnlColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    highlightColor: Color = TextPrimary,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardHighlight, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 9.sp)
            Text(value, color = highlightColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SocialButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(CardHighlight, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = NeonCyan, modifier = Modifier.size(13.dp))
            Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
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
