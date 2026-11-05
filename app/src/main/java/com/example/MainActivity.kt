package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PumpToken
import com.example.ui.MainViewModel
import com.example.ui.components.AlertsDialog
import com.example.ui.components.NotificationBanner
import com.example.ui.components.TradeDialog
import com.example.ui.screens.PortfolioScreen
import com.example.ui.screens.RadarScreen
import com.example.ui.screens.TokenDetailScreen
import com.example.ui.screens.TradingBotScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PumpPulseApp()
            }
        }
    }
}

enum class NavigationTab(val title: String, val icon: ImageVector) {
    RADAR("Radar", Icons.Default.Radar),
    BOT("Auto-Bot", Icons.Default.SmartToy),
    PORTFOLIO("Portfolio", Icons.Default.Analytics)
}

@Composable
fun PumpPulseApp(
    viewModel: MainViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(NavigationTab.RADAR) }
    var tradeToken by remember { mutableStateOf<PumpToken?>(null) }
    var showAlertsDialog by remember { mutableStateOf(false) }

    val filteredTokens by viewModel.filteredTokens.collectAsState()
    val allTokens by viewModel.tokens.collectAsState()
    val selectedTokenId by viewModel.selectedTokenId.collectAsState()
    val selectedToken by viewModel.selectedToken.collectAsState()
    val walletSol by viewModel.walletSolBalance.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val limitOrders by viewModel.limitOrders.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val portfolioSummary by viewModel.portfolioSummary.collectAsState()
    val recentNotification by viewModel.recentNotification.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberBlack,
        bottomBar = {
            // Only show bottom bar when not in full-screen detail view
            if (selectedTokenId == null) {
                NavigationBar(
                    containerColor = DarkNavy,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .border(1.dp, CardBorder)
                        .testTag("bottom_nav_bar")
                ) {
                    NavigationTab.values().forEach { tab ->
                        val isSelected = currentTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) NeonGreen else TextMuted
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) NeonGreen else TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = NeonGreen.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Routing
            if (selectedToken != null) {
                val token = selectedToken!!
                val tokenHolding = holdings.find { it.tokenId == token.id }
                TokenDetailScreen(
                    token = token,
                    walletSol = walletSol,
                    holding = tokenHolding,
                    onBack = { viewModel.selectToken(null) },
                    onOpenTrade = { tradeToken = token },
                    onToggleFavorite = { viewModel.toggleFavorite(token.id) }
                )
            } else {
                when (currentTab) {
                    NavigationTab.RADAR -> {
                        RadarScreen(
                            viewModel = viewModel,
                            tokens = filteredTokens,
                            onSelectToken = { token -> viewModel.selectToken(token.id) },
                            onQuickBuy = { token -> tradeToken = token },
                            onOpenAlerts = { showAlertsDialog = true },
                            onOpenWallet = { currentTab = NavigationTab.PORTFOLIO }
                        )
                    }
                    NavigationTab.BOT -> {
                        TradingBotScreen(
                            viewModel = viewModel,
                            limitOrders = limitOrders,
                            tokens = allTokens
                        )
                    }
                    NavigationTab.PORTFOLIO -> {
                        PortfolioScreen(
                            viewModel = viewModel,
                            portfolioSummary = portfolioSummary,
                            holdings = holdings,
                            transactions = transactions,
                            tokens = allTokens,
                            onSelectToken = { token -> viewModel.selectToken(token.id) }
                        )
                    }
                }
            }

            // Real-Time Floating Notification Banner (Spikes, Dumps, Dex Paid)
            NotificationBanner(
                alert = recentNotification,
                onDismiss = { viewModel.dismissNotification() },
                onSelectToken = { tokenId ->
                    viewModel.selectToken(tokenId)
                    viewModel.dismissNotification()
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Trade Modal Dialog
            if (tradeToken != null) {
                val targetToken = tradeToken!!
                val tokenHolding = holdings.find { it.tokenId == targetToken.id }
                TradeDialog(
                    token = targetToken,
                    walletSol = walletSol,
                    holding = tokenHolding,
                    onDismiss = { tradeToken = null },
                    onBuy = { amtSol -> viewModel.buyToken(targetToken.id, amtSol) },
                    onSell = { amtTokens -> viewModel.sellToken(targetToken.id, amtTokens) },
                    onAddLimitOrder = { type, targetPrice, amtSol, slippage ->
                        viewModel.addLimitOrder(targetToken.id, type, targetPrice, amtSol, slippage)
                    }
                )
            }

            // Alerts History Dialog
            if (showAlertsDialog) {
                AlertsDialog(
                    alerts = alerts,
                    onDismiss = { showAlertsDialog = false },
                    onSelectToken = { tokenId ->
                        viewModel.selectToken(tokenId)
                        showAlertsDialog = false
                    }
                )
            }
        }
    }
}
