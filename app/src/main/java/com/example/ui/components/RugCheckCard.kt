package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RiskLevel
import com.example.data.model.RugAnalysis
import com.example.ui.theme.*

@Composable
fun RugCheckCard(
    rugAnalysis: RugAnalysis,
    modifier: Modifier = Modifier
) {
    val score = rugAnalysis.safetyScore
    val riskLevel = rugAnalysis.riskLevel

    val badgeColor = when (riskLevel) {
        RiskLevel.SAFE -> SafeGreen
        RiskLevel.MODERATE -> WarningAmber
        RiskLevel.HIGH_RISK -> DangerRed
        RiskLevel.UNKNOWN -> WarningAmber
    }

    val badgeBg = when (riskLevel) {
        RiskLevel.SAFE -> SafeGreen.copy(alpha = 0.15f)
        RiskLevel.MODERATE -> WarningAmber.copy(alpha = 0.15f)
        RiskLevel.HIGH_RISK -> DangerRedBg
        RiskLevel.UNKNOWN -> WarningAmber.copy(alpha = 0.15f)
    }

    val riskTitle = when (riskLevel) {
        RiskLevel.SAFE -> "Low Risk (Safe to Trade)"
        RiskLevel.MODERATE -> "Moderate Risk"
        RiskLevel.HIGH_RISK -> "HIGH RISK / DANGER"
        RiskLevel.UNKNOWN -> "NOT VERIFIED"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("rug_check_card"),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(badgeColor.copy(alpha = 0.4f)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Security Icon & Safety Score Gauge
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
                            .background(badgeBg, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security Shield",
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Rug Pull Detector",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = riskTitle,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Safety Score Badge
                Box(
                    modifier = Modifier
                        .background(badgeBg, RoundedCornerShape(10.dp))
                        .border(1.dp, badgeColor, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (rugAnalysis.isVerified) "$score" else "N/A",
                            color = badgeColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "/100",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Critical Security Checks Grid (2 columns)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecurityCheckItem(
                        modifier = Modifier.weight(1f),
                        title = "Mint Authority",
                        statusText = if (!rugAnalysis.isVerified) "Not verified" else if (rugAnalysis.isMintRenounced) "Renounced" else "Active (Danger)",
                        isGood = rugAnalysis.isVerified && rugAnalysis.isMintRenounced
                    )
                    SecurityCheckItem(
                        modifier = Modifier.weight(1f),
                        title = "Freeze Authority",
                        statusText = if (!rugAnalysis.isVerified) "Not verified" else if (rugAnalysis.isFreezeDisabled) "Disabled" else "Active (Danger)",
                        isGood = rugAnalysis.isVerified && rugAnalysis.isFreezeDisabled
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecurityCheckItem(
                        modifier = Modifier.weight(1f),
                        title = "LP Status",
                        statusText = if (!rugAnalysis.isVerified) "Not verified" else if (rugAnalysis.isLpBurned) "Burned/Locked" else "Unlocked",
                        isGood = rugAnalysis.isVerified && rugAnalysis.isLpBurned
                    )
                    SecurityCheckItem(
                        modifier = Modifier.weight(1f),
                        title = "Honeypot Sim",
                        statusText = if (!rugAnalysis.isVerified) "Not verified" else if (!rugAnalysis.honeypotRisk) "Passed (Can Sell)" else "Failed (Tax/Drain)",
                        isGood = rugAnalysis.isVerified && !rugAnalysis.honeypotRisk
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecurityCheckItem(
                        modifier = Modifier.weight(1f),
                        title = "Dev Holding",
                        statusText = if (rugAnalysis.isVerified) "${String.format("%.1f", rugAnalysis.devHoldingPct)}% of supply" else "Not verified",
                        isGood = rugAnalysis.isVerified && rugAnalysis.devHoldingPct <= 5.0
                    )
                    SecurityCheckItem(
                        modifier = Modifier.weight(1f),
                        title = "Top 10 Concentration",
                        statusText = if (rugAnalysis.isVerified) "${String.format("%.1f", rugAnalysis.top10HoldersPct)}%" else "Not verified",
                        isGood = rugAnalysis.isVerified && rugAnalysis.top10HoldersPct <= 35.0
                    )
                }
            }

            // Bundled Snipers note
            if (rugAnalysis.bundledSnipersCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardHighlight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Snipers",
                        tint = if (rugAnalysis.bundledSnipersCount > 5) DangerRed else WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${rugAnalysis.bundledSnipersCount} bundled snipers detected in block 0",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Active Warnings list
            if (rugAnalysis.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rugAnalysis.isVerified) DangerRedBg else WarningAmber.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                        .border(1.dp, (if (rugAnalysis.isVerified) DangerRed else WarningAmber).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = "Warning",
                            tint = DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (rugAnalysis.isVerified) "Security Warnings Detected:" else "Security Checks Not Verified:",
                            color = if (rugAnalysis.isVerified) DangerRed else WarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    rugAnalysis.warnings.forEach { warn ->
                        Text(
                            text = "• $warn",
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityCheckItem(
    title: String,
    statusText: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardHighlight, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isGood) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isGood) SafeGreen else DangerRed,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = statusText,
                    color = if (isGood) TextPrimary else DangerRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
