package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.OrderType
import com.example.data.model.PortfolioHolding
import com.example.data.model.PumpToken
import com.example.ui.theme.*

enum class TradeTab {
    INSTANT_BUY,
    INSTANT_SELL,
    LIMIT_ORDER
}

@Composable
fun TradeDialog(
    token: PumpToken,
    walletSol: Double,
    holding: PortfolioHolding?,
    onDismiss: () -> Unit,
    onBuy: (amountSol: Double) -> Unit,
    onSell: (amountTokens: Double) -> Unit,
    onAddLimitOrder: (type: OrderType, targetPriceSol: Double, amountSol: Double, slippagePct: Double) -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(TradeTab.INSTANT_BUY) }

    // Instant Buy state
    var buyAmountSolText by remember { mutableStateOf("0.25") }

    // Instant Sell state
    val maxTokens = holding?.amountTokens ?: 0.0
    var sellTokensText by remember { mutableStateOf(if (maxTokens > 0) String.format("%.0f", maxTokens) else "0") }

    // Limit Order state
    var selectedOrderType by remember { mutableStateOf(OrderType.TAKE_PROFIT) }
    var targetPriceText by remember(selectedOrderType, token.priceSol) {
        val multiplier = when (selectedOrderType) {
            OrderType.BUY_LIMIT -> 0.85
            OrderType.SELL_LIMIT -> 1.30
            OrderType.TAKE_PROFIT -> 1.50
            OrderType.STOP_LOSS -> 0.75
        }
        mutableStateOf(String.format("%.7f", token.priceSol * multiplier))
    }
    var limitAmountSolText by remember { mutableStateOf("0.25") }
    var slippagePct by remember { mutableStateOf(5.0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("trade_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkNavy),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header with Token Info & Close
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
                                .background(Color(token.avatarBgColor), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(token.avatarEmoji, fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "Trade ${token.symbol}",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${String.format("%.7f", token.priceSol)} SOL ($${String.format("%.6f", token.priceUsd)})",
                                color = NeonGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs (Buy / Sell / Limit Order)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TradeTabButton(
                        label = "⚡ Instant Buy",
                        isSelected = currentTab == TradeTab.INSTANT_BUY,
                        activeColor = NeonGreen,
                        modifier = Modifier.weight(1f)
                    ) { currentTab = TradeTab.INSTANT_BUY }

                    TradeTabButton(
                        label = "🔻 Sell",
                        isSelected = currentTab == TradeTab.INSTANT_SELL,
                        activeColor = DangerRed,
                        modifier = Modifier.weight(1f)
                    ) { currentTab = TradeTab.INSTANT_SELL }

                    TradeTabButton(
                        label = "🎯 Limit Order",
                        isSelected = currentTab == TradeTab.LIMIT_ORDER,
                        activeColor = NeonCyan,
                        modifier = Modifier.weight(1.2f)
                    ) { currentTab = TradeTab.LIMIT_ORDER }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // TAB CONTENT
                when (currentTab) {
                    TradeTab.INSTANT_BUY -> {
                        // Instant Buy View
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount to invest:", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = "Wallet: ${String.format("%.2f", walletSol)} SOL",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = buyAmountSolText,
                            onValueChange = { buyAmountSolText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("buy_amount_input"),
                            trailingIcon = { Text("SOL", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // SOL Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("0.1", "0.25", "0.5", "1.0", "2.0").forEach { preset ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(CardHighlight, RoundedCornerShape(8.dp))
                                        .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                        .clickable { buyAmountSolText = preset }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$preset", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Estimated tokens
                        val solVal = buyAmountSolText.toDoubleOrNull() ?: 0.0
                        val estTokens = if (token.priceSol > 0) solVal / token.priceSol else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("You will receive approx:", color = TextMuted, fontSize = 11.sp)
                            Text(
                                text = "${String.format("%,.0f", estTokens)} ${token.symbol}",
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (solVal <= 0.0) {
                                    Toast.makeText(context, "Please enter valid SOL amount", Toast.LENGTH_SHORT).show()
                                } else if (solVal > walletSol) {
                                    Toast.makeText(context, "Insufficient SOL balance", Toast.LENGTH_SHORT).show()
                                } else {
                                    onBuy(solVal)
                                    Toast.makeText(context, "Swapped $solVal SOL for ${token.symbol}!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("confirm_buy_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = CyberBlack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Instant Buy ${token.symbol}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }

                    TradeTab.INSTANT_SELL -> {
                        // Instant Sell View
                        if (holding == null || holding.amountTokens <= 0.0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("You do not hold any ${token.symbol} tokens yet.", color = TextMuted, fontSize = 13.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tokens to sell:", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    text = "Holding: ${String.format("%,.0f", maxTokens)}",
                                    color = DexPaidGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = sellTokensText,
                                onValueChange = { sellTokensText = it },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Text(token.symbol, color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = DangerRed,
                                    unfocusedBorderColor = CardBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // % Quick Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(25, 50, 75, 100).forEach { pct ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(CardHighlight, RoundedCornerShape(8.dp))
                                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                val amt = (maxTokens * pct / 100.0)
                                                sellTokensText = String.format("%.0f", amt)
                                            }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$pct%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val tokensToSell = sellTokensText.toDoubleOrNull() ?: 0.0
                            val estSolReceived = tokensToSell * token.priceSol
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Est. SOL Return:", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = "+${String.format("%.4f", estSolReceived)} SOL ($${String.format("%.2f", estSolReceived * 185.5)})",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (tokensToSell <= 0) {
                                        Toast.makeText(context, "Please enter valid tokens amount", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onSell(tokensToSell)
                                        Toast.makeText(context, "Sold ${String.format("%,.0f", tokensToSell)} ${token.symbol}!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("confirm_sell_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DangerRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Sell ${token.symbol}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }
                        }
                    }

                    TradeTab.LIMIT_ORDER -> {
                        // Limit Order Configuration
                        Text("Order Strategy Type:", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OrderType.values().forEach { type ->
                                val isSelected = selectedOrderType == type
                                val typeLabel = when (type) {
                                    OrderType.BUY_LIMIT -> "Buy Limit"
                                    OrderType.SELL_LIMIT -> "Sell Limit"
                                    OrderType.TAKE_PROFIT -> "Take Profit"
                                    OrderType.STOP_LOSS -> "Stop Loss"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else CardHighlight, RoundedCornerShape(6.dp))
                                        .border(1.dp, if (isSelected) NeonCyan else CardBorder, RoundedCornerShape(6.dp))
                                        .clickable { selectedOrderType = type }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = typeLabel,
                                        color = if (isSelected) NeonCyan else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Target Price Input
                        Text("Target Trigger Price (SOL):", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = targetPriceText,
                            onValueChange = { targetPriceText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Investment Amount in SOL
                        Text("Amount (SOL):", color = TextSecondary, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = limitAmountSolText,
                            onValueChange = { limitAmountSolText = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = CardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val target = targetPriceText.toDoubleOrNull()
                                val amt = limitAmountSolText.toDoubleOrNull()
                                if (target == null || amt == null || target <= 0 || amt <= 0) {
                                    Toast.makeText(context, "Invalid limit order parameters", Toast.LENGTH_SHORT).show()
                                } else {
                                    onAddLimitOrder(selectedOrderType, target, amt, slippagePct)
                                    Toast.makeText(context, "Limit Order Placed! Bot active.", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("place_limit_order_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan,
                                contentColor = CyberBlack
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Activate Limit Order Bot", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeTabButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) activeColor else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
