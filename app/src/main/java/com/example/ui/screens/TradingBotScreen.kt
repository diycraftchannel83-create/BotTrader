package com.example.ui.screens

import com.example.util.TokenPriceFormatter

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BotStrategyConfig
import com.example.data.model.LimitOrder
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PumpToken
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun TradingBotScreen(
    viewModel: MainViewModel,
    limitOrders: List<LimitOrder>,
    tokens: List<PumpToken>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val botConfig by viewModel.botConfig.collectAsState()
    val candidateAssessments by viewModel.candidateAssessments.collectAsState()
    val autoTradeStatus by viewModel.autoTradeStatus.collectAsState()

    var showConfigModal by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "bot_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "pulseGlow"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CyberBlack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("trading_bot_screen"),
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
                            text = "AUTOMATED TRADING BOT",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Smart Auto-Buy / Auto-Sell • Paper Mode",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Configure Strategy Button
                    Button(
                        onClick = { showConfigModal = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CardHighlight,
                            contentColor = NeonCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Strategy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Master Control Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkNavy),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (botConfig.isMasterEnabled) NeonPurple else CardBorder
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (botConfig.isMasterEnabled) NeonPurple.copy(alpha = pulseGlow) else TextMuted
                                        )
                                )
                                Column {
                                    Text(
                                        text = if (botConfig.isMasterEnabled) "Paper Engine: RUNNING" else "Paper Engine: PAUSED",
                                        color = if (botConfig.isMasterEnabled) NeonPurple else TextMuted,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Live DEX market data • Paper orders only",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = botConfig.isMasterEnabled,
                                onCheckedChange = { viewModel.toggleBot(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = CyberBlack,
                                    checkedTrackColor = NeonPurple,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = CardHighlight
                                ),
                                modifier = Modifier.testTag("bot_master_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current Active Rules Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StrategyBadge(
                                label = "Smart Auto-Buy",
                                value = if (botConfig.autoBuyEnabled) "ON (${botConfig.buyAmountSol} SOL)" else "OFF",
                                isGreen = botConfig.autoBuyEnabled,
                                modifier = Modifier.weight(1f)
                            )
                            StrategyBadge(
                                label = "Min Score",
                                value = "${botConfig.minCandidateScore}/100",
                                isGreen = true,
                                modifier = Modifier.weight(1f)
                            )
                            StrategyBadge(
                                label = "Auto-Sell",
                                value = if (botConfig.autoSellEnabled) "ON" else "OFF",
                                isGreen = botConfig.autoSellEnabled,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("SMART SCANNER", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(autoTradeStatus, color = TextSecondary, fontSize = 11.sp)
                        Text("0.01 SOL per entry • Max ${botConfig.maxOpenPositions} positions • Daily loss cap ${botConfig.maxDailyLossSol} SOL", color = TextMuted, fontSize = 10.sp)
                        Text("500x is not predictable. Scores rank market conditions and do not forecast returns.", color = WarningAmber, fontSize = 10.sp)
                        candidateAssessments.take(5).forEach { candidate ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(candidate.symbol, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(candidate.blockers.firstOrNull() ?: candidate.reasons.take(2).joinToString(" • "), color = TextMuted, fontSize = 9.sp, maxLines = 2)
                                }
                                Text("${candidate.score}/100", color = if (candidate.eligible) NeonGreen else WarningAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            // Active Orders Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Limit Orders (${limitOrders.count { it.status == OrderStatus.ACTIVE }})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Orders List
            if (limitOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardBackground, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No active limit orders", color = TextMuted, fontSize = 13.sp)
                            Text("Set limit orders from the Radar or Token Detail screen", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                items(limitOrders, key = { it.id }) { order ->
                    val token = tokens.find { it.id == order.tokenId }
                    val currentPriceSol = token?.priceSol ?: order.targetPriceSol
                    val distancePct = ((currentPriceSol - order.targetPriceSol) / order.targetPriceSol) * 100.0

                    LimitOrderItemCard(
                        order = order,
                        currentPriceSol = currentPriceSol,
                        distancePct = distancePct,
                        onCancel = {
                            viewModel.cancelLimitOrder(order.id)
                            Toast.makeText(context, "Order Cancelled", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Strategy Configuration Modal
    if (showConfigModal) {
        BotConfigDialog(
            currentConfig = botConfig,
            onDismiss = { showConfigModal = false },
            onSave = { updated ->
                viewModel.updateBotConfig(updated)
                showConfigModal = false
                Toast.makeText(context, "Bot strategy settings saved!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun StrategyBadge(
    label: String,
    value: String,
    isGreen: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardHighlight, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 9.sp)
            Text(
                text = value,
                color = if (isGreen) NeonGreen else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LimitOrderItemCard(
    order: LimitOrder,
    currentPriceSol: Double,
    distancePct: Double,
    onCancel: () -> Unit
) {
    val isFilled = order.status == OrderStatus.FILLED
    val isCancelled = order.status == OrderStatus.CANCELLED
    val isActive = order.status == OrderStatus.ACTIVE

    val statusColor = when (order.status) {
        OrderStatus.ACTIVE -> NeonCyan
        OrderStatus.FILLED -> NeonGreen
        OrderStatus.CANCELLED -> TextMuted
        OrderStatus.TRIGGERED -> DexPaidGold
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isActive) NeonCyan.copy(alpha = 0.35f) else CardBorder
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
                    val typeColor = when (order.orderType) {
                        OrderType.BUY_LIMIT -> NeonGreen
                        OrderType.SELL_LIMIT -> DangerRed
                        OrderType.TAKE_PROFIT -> DexPaidGold
                        OrderType.STOP_LOSS -> DangerRed
                    }
                    Box(
                        modifier = Modifier
                            .background(typeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .border(1.dp, typeColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(order.orderType.name.replace("_", " "), color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(order.tokenSymbol, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(order.status.name, color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Target Trigger Price", color = TextMuted, fontSize = 10.sp)
                    Text("${TokenPriceFormatter.format(order.targetPriceSol)} SOL", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Current Price", color = TextMuted, fontSize = 10.sp)
                    Text("${TokenPriceFormatter.format(currentPriceSol)} SOL", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Distance", color = TextMuted, fontSize = 10.sp)
                    Text(
                        text = "${if (distancePct >= 0) "+" else ""}${String.format("%.1f", distancePct)}%",
                        color = if (kotlin.math.abs(distancePct) < 5.0) NeonGreen else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isFilled && order.executionNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("✓ ${order.executionNote}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCancel,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Cancel Order", color = DangerRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BotConfigDialog(
    currentConfig: BotStrategyConfig,
    onDismiss: () -> Unit,
    onSave: (BotStrategyConfig) -> Unit
) {
    var autoBuy by remember { mutableStateOf(currentConfig.autoBuyEnabled) }
    var autoSell by remember { mutableStateOf(currentConfig.autoSellEnabled) }
    var allowUnverified by remember { mutableStateOf(currentConfig.allowUnverifiedSecurityData) }
    var buyAmountText by remember { mutableStateOf(currentConfig.buyAmountSol.toString()) }
    var minScoreText by remember { mutableStateOf(currentConfig.minCandidateScore.toString()) }
    var liquidityWeightText by remember { mutableStateOf(currentConfig.scoreLiquidityWeight.toString()) }
    var volumeWeightText by remember { mutableStateOf(currentConfig.scoreVolumeWeight.toString()) }
    var momentumWeightText by remember { mutableStateOf(currentConfig.scoreMomentumWeight.toString()) }
    var ageWeightText by remember { mutableStateOf(currentConfig.scoreAgeWeight.toString()) }
    var marketCapWeightText by remember { mutableStateOf(currentConfig.scoreMarketCapWeight.toString()) }
    var trendWeightText by remember { mutableStateOf(currentConfig.scoreTrendWeight.toString()) }
    var minLiquidityText by remember { mutableStateOf(currentConfig.minLiquidityUsd.toString()) }
    var maxPositionsText by remember { mutableStateOf(currentConfig.maxOpenPositions.toString()) }
    var dailyLossText by remember { mutableStateOf(currentConfig.maxDailyLossSol.toString()) }
    var trailingStopText by remember { mutableStateOf(currentConfig.trailingStopPctAuto.toString()) }
    var takeProfitText by remember { mutableStateOf(currentConfig.defaultTakeProfitPct.toString()) }
    var stopLossText by remember { mutableStateOf(currentConfig.defaultStopLossPct.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Bot Strategy", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Smart Auto-Buy", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Automatically buy eligible candidates in paper mode", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(checked = autoBuy, onCheckedChange = { autoBuy = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Automatic Exit", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Stop-loss, take-profit and trailing stop", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(checked = autoSell, onCheckedChange = { autoSell = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Allow unverified security data", color = WarningAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("HIGH RISK: current feed cannot verify authorities or holders", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(checked = allowUnverified, onCheckedChange = { allowUnverified = it })
                }


                OutlinedTextField(value = buyAmountText, onValueChange = { buyAmountText = it }, label = { Text("Buy amount per token (SOL) — default 0.01") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = minScoreText, onValueChange = { minScoreText = it }, label = { Text("Min score (0-100)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxPositionsText, onValueChange = { maxPositionsText = it }, label = { Text("Max positions") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                Text("EDIT SCORING FORMULA", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                Text("Set the relative importance of each factor. Weights are normalized automatically; they do not need to add up to 100. At least one weight must be above zero.", color = TextMuted, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = liquidityWeightText, onValueChange = { liquidityWeightText = it }, label = { Text("Liquidity weight") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = volumeWeightText, onValueChange = { volumeWeightText = it }, label = { Text("Volume weight") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = momentumWeightText, onValueChange = { momentumWeightText = it }, label = { Text("Momentum weight") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ageWeightText, onValueChange = { ageWeightText = it }, label = { Text("Token age weight") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = marketCapWeightText, onValueChange = { marketCapWeightText = it }, label = { Text("Market cap weight") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = trendWeightText, onValueChange = { trendWeightText = it }, label = { Text("Trend weight") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(value = minLiquidityText, onValueChange = { minLiquidityText = it }, label = { Text("Minimum liquidity (USD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dailyLossText, onValueChange = { dailyLossText = it }, label = { Text("Daily loss cap (SOL)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = trailingStopText, onValueChange = { trailingStopText = it }, label = { Text("Trailing stop %") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                // Default Take Profit & Stop Loss
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = takeProfitText,
                        onValueChange = { takeProfitText = it },
                        label = { Text("Take Profit %") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = { stopLossText = it },
                        label = { Text("Stop Loss %") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val configuredWeights = listOf(
                        liquidityWeightText, volumeWeightText, momentumWeightText,
                        ageWeightText, marketCapWeightText, trendWeightText
                    ).map { it.toIntOrNull()?.coerceIn(0, 100) ?: 0 }
                    if (configuredWeights.sum() == 0) {
                        Toast.makeText(context, "At least one scoring weight must be greater than zero.", Toast.LENGTH_LONG).show()
                        return@Button
                    }
                    val updated = currentConfig.copy(
                        autoBuyEnabled = autoBuy,
                        autoSellEnabled = autoSell,
                        allowUnverifiedSecurityData = allowUnverified,
                        buyAmountSol = buyAmountText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: currentConfig.buyAmountSol,
                        minCandidateScore = minScoreText.toIntOrNull()?.coerceIn(0, 100) ?: currentConfig.minCandidateScore,
                        scoreLiquidityWeight = liquidityWeightText.toIntOrNull()?.coerceIn(0, 100) ?: currentConfig.scoreLiquidityWeight,
                        scoreVolumeWeight = volumeWeightText.toIntOrNull()?.coerceIn(0, 100) ?: currentConfig.scoreVolumeWeight,
                        scoreMomentumWeight = momentumWeightText.toIntOrNull()?.coerceIn(0, 100) ?: currentConfig.scoreMomentumWeight,
                        scoreAgeWeight = ageWeightText.toIntOrNull()?.coerceIn(0, 100) ?: currentConfig.scoreAgeWeight,
                        scoreMarketCapWeight = marketCapWeightText.toIntOrNull()?.coerceIn(0, 100) ?: currentConfig.scoreMarketCapWeight,
                        scoreTrendWeight = trendWeightText.toIntOrNull()?.coerceIn(0, 100) ?: currentConfig.scoreTrendWeight,
                        minLiquidityUsd = minLiquidityText.toDoubleOrNull()?.takeIf { it >= 0.0 } ?: currentConfig.minLiquidityUsd,
                        maxOpenPositions = maxPositionsText.toIntOrNull()?.coerceIn(1, 20) ?: currentConfig.maxOpenPositions,
                        maxDailyLossSol = dailyLossText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: currentConfig.maxDailyLossSol,
                        trailingStopPctAuto = trailingStopText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: currentConfig.trailingStopPctAuto,
                        takeProfitPct = takeProfitText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: currentConfig.takeProfitPct,
                        stopLossPct = stopLossText.toDoubleOrNull()?.takeIf { it > 0.0 } ?: currentConfig.stopLossPct
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = CyberBlack)
            ) {
                Text("Save Strategy", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkNavy
    )
}
