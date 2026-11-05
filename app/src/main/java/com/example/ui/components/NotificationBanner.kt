package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertType
import com.example.data.model.PriceAlert
import com.example.ui.theme.*

@Composable
fun NotificationBanner(
    alert: PriceAlert?,
    onDismiss: () -> Unit,
    onSelectToken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = alert != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (alert != null) {
            val accentColor = when (alert.type) {
                AlertType.PRICE_SURGE -> NeonGreen
                AlertType.PRICE_DUMP -> DangerRed
                AlertType.DEX_PAID -> DexPaidGold
                AlertType.GRADUATION -> NeonCyan
                AlertType.RUG_WARNING -> DangerRed
                AlertType.BOT_TRIGGER -> NeonPurple
                AlertType.WITHDRAWAL -> NeonCyan
                AlertType.WALLET_EVENT -> NeonGreen
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(DarkNavy, RoundedCornerShape(14.dp))
                    .border(1.5.dp, accentColor, RoundedCornerShape(14.dp))
                    .clickable { onSelectToken(alert.tokenId) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("notification_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = alert.message,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            maxLines = 2
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
