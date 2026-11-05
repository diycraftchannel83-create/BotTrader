package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PortfolioHolding
import com.example.data.model.PumpToken
import com.example.data.model.TradeTransaction
import com.example.ui.MainViewModel
import com.example.ui.PortfolioSummary
import com.example.ui.components.ImportWalletDialog
import com.example.ui.components.WithdrawDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PortfolioScreen(
    viewModel: MainViewModel,
    portfolioSummary: PortfolioSummary,
    holdings: List<PortfolioHolding>,
    transactions: List<TradeTransaction>,
    tokens: List<PumpToken>,
    onSelectToken: (PumpToken) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentWallet by viewModel.currentWallet.collectAsState()
    val walletSol by viewModel.walletSolBalance.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Holdings, 1: Trade History
    var showImportDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("portfolio_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PORTFOLIO ANALYTICS",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Comprehensive Asset & PnL Performance",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Reset Wallet Button
                    OutlinedButton(
                        onClick = {
                            viewModel.resetWallet()
                            Toast.makeText(context, "Wallet reset to 5.0 SOL", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Solana Wallet Management Card (Import & Withdraw)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("solana_wallet_card"),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (currentWallet.isImported) NeonGreen.copy(alpha = 0.5f) else CardBorder
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (currentWallet.isImported) NeonGreen.copy(alpha = 0.15f) else CardHighlight,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (currentWallet.isImported) Icons.Default.VpnKey else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (currentWallet.isImported) NeonGreen else NeonCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(currentWallet.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (currentWallet.isImported) NeonGreen.copy(alpha = 0.15f) else CardHighlight,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (currentWallet.isImported) "IMPORTED" else "LOCAL",
                                                color = if (currentWallet.isImported) NeonGreen else TextMuted,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    // Clickable short address to copy
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Solana Address", currentWallet.address)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Wallet address copied!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(currentWallet.shortAddress, color = TextMuted, fontSize = 11.sp)
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(11.dp))
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("Available", color = TextMuted, fontSize = 10.sp)
                                Text(
                                    text = "${String.format("%.4f", walletSol)} SOL",
                                    color = NeonGreen,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: Import Key & Withdraw Function Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Import Wallet Button
                            OutlinedButton(
                                onClick = { showImportDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("import_wallet_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (currentWallet.isImported) NeonGreen else TextPrimary
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (currentWallet.isImported) NeonGreen else CardBorder
                                    )
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentWallet.isImported) "Change Key" else "Import Key",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Withdraw Function Button
                            Button(
                                onClick = { showWithdrawDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("withdraw_sol_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = CyberBlack
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Withdraw", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Total Portfolio Value Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(18.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Total Portfolio Net Worth", color = TextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$${String.format("%,.2f", portfolioSummary.totalBalanceUsd)}",
                                color = TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(
                                modifier = Modifier
                                    .background(NeonGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .border(1.dp, NeonGreen, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${String.format("%.3f", portfolioSummary.totalBalanceSol)} SOL",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PnL & Win Rate Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val pnlSol = portfolioSummary.unrealizedPnlSol
                            val pnlColor = if (pnlSol >= 0) NeonGreen else DangerRed
                            AnalyticsMetricPill(
                                label = "Unrealized PnL",
                                value = "${if (pnlSol >= 0) "+" else ""}${String.format("%.4f", pnlSol)} SOL",
                                subValue = "(${String.format("%.1f", portfolioSummary.unrealizedPnlPct)}%)",
                                color = pnlColor,
                                modifier = Modifier.weight(1.2f)
                            )

                            val realPnl = portfolioSummary.realizedPnlSol
                            val realColor = if (realPnl >= 0) NeonGreen else DangerRed
                            AnalyticsMetricPill(
                                label = "Realized PnL",
                                value = "${if (realPnl >= 0) "+" else ""}${String.format("%.4f", realPnl)} SOL",
                                subValue = "Closed trades",
                                color = realColor,
                                modifier = Modifier.weight(1.1f)
                            )

                            AnalyticsMetricPill(
                                label = "Win Rate",
                                value = "${String.format("%.0f", portfolioSummary.winRatePct)}%",
                                subValue = "${portfolioSummary.winCount}/${portfolioSummary.totalTrades} wins",
                                color = DexPaidGold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Tabs: Holdings vs Trade History
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TabButton(
                        title = "Holdings (${holdings.size})",
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 0 }

                    TabButton(
                        title = "Activity & Transfers (${transactions.size})",
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f)
                    ) { selectedTab = 1 }
                }
            }

            // Tab Content
            if (selectedTab == 0) {
                // Holdings List
                if (holdings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground, RoundedCornerShape(12.dp))
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No open token holdings", color = TextMuted, fontSize = 13.sp)
                                Text("Snipe tokens on the radar to build your portfolio", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    items(holdings, key = { it.tokenId }) { holding ->
                        val token = tokens.find { it.id == holding.tokenId }
                        val curPriceSol = token?.priceSol ?: holding.entryPriceSol
                        val pnlSol = holding.pnlSol(curPriceSol)
                        val pnlPct = holding.pnlPct(curPriceSol)
                        val pnlColor = if (pnlSol >= 0) NeonGreen else DangerRed

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(14.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(CardHighlight, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(holding.avatarEmoji, fontSize = 18.sp)
                                        }
                                        Column {
                                            Text(holding.tokenSymbol, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text(holding.tokenName, color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${String.format("%.4f", holding.currentValSol(curPriceSol))} SOL",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$${String.format("%.2f", holding.currentValSol(curPriceSol) * 185.5)}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Entry / Current", color = TextMuted, fontSize = 10.sp)
                                        Text(
                                            text = "${String.format("%.7f", holding.entryPriceSol)} → ${String.format("%.7f", curPriceSol)}",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("PnL", color = TextMuted, fontSize = 10.sp)
                                        Text(
                                            text = "${if (pnlSol >= 0) "+" else ""}${String.format("%.4f", pnlSol)} SOL (${String.format("%.1f", pnlPct)}%)",
                                            color = pnlColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Quick Sell All Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.sellToken(holding.tokenId, holding.amountTokens)
                                            Toast.makeText(context, "Closed holding on ${holding.tokenSymbol}!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Sell All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Trade & Transfer History
                if (transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardBackground, RoundedCornerShape(12.dp))
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No trades or transfers executed yet", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(transactions, key = { it.id }) { tx ->
                        val isBuy = tx.type == "BUY"
                        val isWithdraw = tx.type == "WITHDRAW"
                        val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        val timeStr = dateFormat.format(Date(tx.timestamp))

                        val badgeColor = when {
                            isWithdraw -> NeonCyan
                            isBuy -> NeonGreen
                            else -> DangerRed
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = tx.type,
                                            color = badgeColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(tx.tokenSymbol, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            if (tx.isAutoBot) {
                                                Text("🤖 Bot", color = NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (isWithdraw && tx.recipientAddress != null) {
                                            val shortRec = if (tx.recipientAddress.length > 8)
                                                "${tx.recipientAddress.take(4)}...${tx.recipientAddress.takeLast(4)}"
                                            else tx.recipientAddress
                                            Text("To: $shortRec • $timeStr", color = TextMuted, fontSize = 10.sp)
                                        } else {
                                            Text(timeStr, color = TextMuted, fontSize = 10.sp)
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val amountPrefix = when {
                                        isWithdraw -> "-"
                                        isBuy -> "-"
                                        else -> "+"
                                    }
                                    Text(
                                        text = "$amountPrefix${String.format("%.4f", tx.amountSol)} SOL",
                                        color = if (isWithdraw) NeonCyan else if (isBuy) TextPrimary else NeonGreen,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (tx.realizedPnlSol != null) {
                                        val pnl = tx.realizedPnlSol
                                        val pnlColor = if (pnl >= 0) NeonGreen else DangerRed
                                        Text(
                                            text = "PnL: ${if (pnl >= 0) "+" else ""}${String.format("%.4f", pnl)} SOL",
                                            color = pnlColor,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Import Wallet Dialog
    if (showImportDialog) {
        ImportWalletDialog(
            currentWallet = currentWallet,
            onDismiss = { showImportDialog = false },
            onImport = { key, label ->
                viewModel.importPrivateKey(key, label)
            },
            onDisconnect = {
                viewModel.disconnectWallet()
            }
        )
    }

    // Withdraw Dialog
    if (showWithdrawDialog) {
        WithdrawDialog(
            currentWallet = currentWallet,
            availableSol = walletSol,
            onDismiss = { showWithdrawDialog = false },
            onWithdraw = { recipient, amount ->
                viewModel.withdrawSol(recipient, amount)
            }
        )
    }
}

@Composable
private fun AnalyticsMetricPill(
    label: String,
    value: String,
    subValue: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardHighlight, RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 9.sp)
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(subValue, color = TextMuted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (isSelected) CardHighlight else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) NeonGreen else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
