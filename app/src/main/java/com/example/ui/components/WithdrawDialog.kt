package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.SolanaWallet
import com.example.ui.theme.*
import com.example.util.SolanaKeyUtils

@Composable
fun WithdrawDialog(
    currentWallet: SolanaWallet,
    availableSol: Double,
    onDismiss: () -> Unit,
    onWithdraw: (recipientAddress: String, amountSol: Double) -> Unit
) {
    val context = LocalContext.current
    var destinationAddressText by remember { mutableStateOf("") }
    var amountSolText by remember { mutableStateOf("1.0") }

    val networkFee = 0.000005
    val amountVal = amountSolText.toDoubleOrNull() ?: 0.0
    val totalDeduction = amountVal + networkFee
    val remainingBalance = (availableSol - totalDeduction).coerceAtLeast(0.0)

    val isValidAddress = remember(destinationAddressText, currentWallet.address) {
        val trimmed = destinationAddressText.trim()
        SolanaKeyUtils.isValidSolanaAddress(trimmed) && trimmed != currentWallet.address
    }

    val isSufficientBalance = amountVal > 0 && totalDeduction <= availableSol

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("withdraw_dialog"),
            colors = CardDefaults.cardColors(containerColor = DarkNavy),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
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
                                .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("Withdraw SOL", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Transfer to external Solana address", color = TextSecondary, fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Source Wallet & Available Balance
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("From Wallet", color = TextMuted, fontSize = 10.sp)
                            Text(currentWallet.shortAddress, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Available Balance", color = TextMuted, fontSize = 10.sp)
                            Text(
                                text = "${String.format("%.4f", availableSol)} SOL",
                                color = NeonGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Destination Address Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recipient Solana Address", color = TextSecondary, fontSize = 12.sp)
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    destinationAddressText = text.trim()
                                    Toast.makeText(context, "Pasted destination address", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = destinationAddressText,
                    onValueChange = { destinationAddressText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("destination_address_input"),
                    placeholder = { Text("e.g. 9WzDXwBbmkg8ZTbNMqUxv...", color = TextMuted, fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (destinationAddressText.isNotBlank() && !isValidAddress) DangerRed else NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                if (destinationAddressText.isNotBlank() && !isValidAddress) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Invalid Solana address (Base58 32-44 chars required)", color = DangerRed, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount SOL Input
                Text("Withdrawal Amount (SOL)", color = TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountSolText,
                    onValueChange = { amountSolText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("withdraw_amount_input"),
                    trailingIcon = { Text("SOL", color = TextMuted, modifier = Modifier.padding(end = 12.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (!isSufficientBalance && amountSolText.isNotBlank()) DangerRed else NeonCyan,
                        unfocusedBorderColor = CardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Amount Presets (25%, 50%, 75%, MAX)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(25, 50, 75).forEach { pct ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(CardHighlight, RoundedCornerShape(8.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    val amt = (availableSol * pct / 100.0)
                                    amountSolText = String.format("%.3f", amt)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$pct%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                            .clickable {
                                val maxWithdrawable = (availableSol - networkFee).coerceAtLeast(0.0)
                                amountSolText = String.format("%.4f", maxWithdrawable)
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("MAX", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Transaction Fee & Summary Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Network Gas Fee:", color = TextMuted, fontSize = 11.sp)
                        Text("${String.format("%.6f", networkFee)} SOL (~$0.0009)", color = TextSecondary, fontSize = 11.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total to deduct:", color = TextMuted, fontSize = 11.sp)
                        Text("${String.format("%.4f", totalDeduction)} SOL", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Remaining Balance:", color = TextMuted, fontSize = 11.sp)
                        Text("${String.format("%.4f", remainingBalance)} SOL", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Confirm Withdrawal Button
                val isFormValid = isValidAddress && isSufficientBalance
                Button(
                    onClick = {
                        if (!isValidAddress) {
                            Toast.makeText(context, "Please enter a valid Solana recipient address", Toast.LENGTH_SHORT).show()
                        } else if (!isSufficientBalance) {
                            Toast.makeText(context, "Insufficient SOL balance", Toast.LENGTH_SHORT).show()
                        } else {
                            onWithdraw(destinationAddressText.trim(), amountVal)
                            Toast.makeText(context, "Withdrawal submitted to Solana cluster!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("confirm_withdraw_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = CyberBlack,
                        disabledContainerColor = CardHighlight,
                        disabledContentColor = TextMuted
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Confirm Withdrawal", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }
    }
}
