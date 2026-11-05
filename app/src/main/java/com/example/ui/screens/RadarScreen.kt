package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DexPaidStatus
import com.example.data.model.PumpToken
import com.example.data.repository.MarketFeedStatus
import com.example.ui.MainViewModel
import com.example.ui.TokenFilter
import com.example.ui.TokenSort
import com.example.ui.components.TokenCard
import com.example.ui.theme.*

@Composable
fun RadarScreen(
    viewModel: MainViewModel,
    tokens: List<PumpToken>,
    onSelectToken: (PumpToken) -> Unit,
    onQuickBuy: (PumpToken) -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenWallet: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val activeSort by viewModel.activeSort.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val botConfig by viewModel.botConfig.collectAsState()
    val currentWallet by viewModel.currentWallet.collectAsState()
    val walletSol by viewModel.walletSolBalance.collectAsState()
    val solPriceUsd by viewModel.solPriceUsd.collectAsState()
    val marketFeedStatus by viewModel.marketFeedStatus.collectAsState()
    val marketError by viewModel.marketError.collectAsState()

    // Pulsing live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.refreshMarketData() },
                containerColor = NeonGreen,
                contentColor = CyberBlack,
                icon = { Icon(Icons.Default.Refresh, contentDescription = "Refresh market data") },
                text = { Text("Refresh Market", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("market_refresh_button")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background((when (marketFeedStatus) { MarketFeedStatus.LIVE -> NeonGreen; MarketFeedStatus.CONNECTING -> NeonCyan; MarketFeedStatus.STALE, MarketFeedStatus.ERROR -> WarningAmber }).copy(alpha = pulseAlpha))
                    )
                    Text(
                        text = "SOLANA MARKET RADAR",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Wallet Chip (Clickable to open Portfolio / Withdraw / Import)
                    Box(
                        modifier = Modifier
                            .background(if (currentWallet.isImported) NeonGreen.copy(alpha = 0.15f) else CardHighlight, RoundedCornerShape(8.dp))
                            .border(1.dp, if (currentWallet.isImported) NeonGreen else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { onOpenWallet() }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("radar_wallet_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (currentWallet.isImported) Icons.Default.VpnKey else Icons.Default.AccountBalanceWallet,
                                contentDescription = "Wallet",
                                tint = if (currentWallet.isImported) NeonGreen else NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${String.format("%.2f", walletSol)} SOL",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bot indicator status
                    Box(
                        modifier = Modifier
                            .background(if (botConfig.isMasterEnabled) NeonPurple.copy(alpha = 0.2f) else CardHighlight, RoundedCornerShape(8.dp))
                            .border(1.dp, if (botConfig.isMasterEnabled) NeonPurple else CardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (botConfig.isMasterEnabled) "🤖 BOT ON" else "🤖 BOT OFF",
                            color = if (botConfig.isMasterEnabled) NeonPurple else TextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Alerts Bell with Badge
                    IconButton(
                        onClick = onOpenAlerts,
                        modifier = Modifier.testTag("alerts_bell_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (alerts.isNotEmpty()) {
                                    Badge(
                                        containerColor = DangerRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("${alerts.size.coerceAtMost(99)}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Alerts",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }

            // Market Stats Header Row
            val verifiedSecurityCount = tokens.count { it.rugAnalysis.isVerified }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    label = "Monitored",
                    value = "${tokens.size}",
                    color = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Security Verified",
                    value = "$verifiedSecurityCount",
                    color = WarningAmber,
                    modifier = Modifier.weight(1.2f)
                )
                StatPill(
                    label = "SOL Price",
                    value = if (solPriceUsd > 0.0) "\${String.format("%.2f", solPriceUsd)}" else "—",
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = when (marketFeedStatus) {
                    MarketFeedStatus.CONNECTING -> "DEXSCREENER • CONNECTING"
                    MarketFeedStatus.LIVE -> "DEXSCREENER • LIVE MARKET DATA"
                    MarketFeedStatus.STALE -> "DEXSCREENER • STALE DATA"
                    MarketFeedStatus.ERROR -> "DEXSCREENER • DATA UNAVAILABLE"
                },
                color = when (marketFeedStatus) {
                    MarketFeedStatus.LIVE -> NeonGreen
                    MarketFeedStatus.CONNECTING -> NeonCyan
                    MarketFeedStatus.STALE, MarketFeedStatus.ERROR -> WarningAmber
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
            if (!marketError.isNullOrBlank() && marketFeedStatus != MarketFeedStatus.LIVE) {
                Text(
                    text = marketError.orEmpty(),
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by token name, symbol, or CA...", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("token_search_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = CardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChipItem(
                        label = "All Launches",
                        isSelected = activeFilter == TokenFilter.ALL,
                        onClick = { viewModel.setFilter(TokenFilter.ALL) }
                    )
                }
                item {
                    FilterChipItem(
                        label = "DEX Listed",
                        isSelected = activeFilter == TokenFilter.DEX_LISTED,
                        onClick = { viewModel.setFilter(TokenFilter.DEX_LISTED) }
                    )
                }
                item {
                    FilterChipItem(
                        label = "🔥 Top Gainers",
                        isSelected = activeFilter == TokenFilter.TOP_GAINERS,
                        onClick = { viewModel.setFilter(TokenFilter.TOP_GAINERS) }
                    )
                }
                item {
                    FilterChipItem(
                        label = "🛡️ Safe (Rug > 80)",
                        isSelected = activeFilter == TokenFilter.SAFE_ONLY,
                        onClick = { viewModel.setFilter(TokenFilter.SAFE_ONLY) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Token List
            if (tokens.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "No tokens",
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(if (marketFeedStatus == MarketFeedStatus.CONNECTING) "Loading live Solana pairs..." else if (marketFeedStatus == MarketFeedStatus.ERROR) "Market data is unavailable. Tap Refresh Market to retry." else "No tokens match your search filter", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tokens_lazy_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tokens, key = { it.id }) { token ->
                        TokenCard(
                            token = token,
                            onClick = { onSelectToken(token) },
                            onQuickBuy = { onQuickBuy(token) },
                            onToggleFavorite = { viewModel.toggleFavorite(token.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardBackground, RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 9.sp)
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    activeBorderColor: Color = NeonGreen,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(if (isSelected) activeBorderColor.copy(alpha = 0.15f) else CardBackground, RoundedCornerShape(8.dp))
            .border(1.dp, if (isSelected) activeBorderColor else CardBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) activeBorderColor else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
