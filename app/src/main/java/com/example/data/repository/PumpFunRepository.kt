package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

enum class MarketFeedStatus { CONNECTING, LIVE, STALE, ERROR }

class PumpFunRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val marketDataSource: DexScreenerDataSource = DexScreenerDataSource()
) {
    private val _solPriceUsd = MutableStateFlow(0.0)
    val solPriceUsd: StateFlow<Double> = _solPriceUsd.asStateFlow()

    private val _tokens = MutableStateFlow<List<PumpToken>>(emptyList())
    val tokens: StateFlow<List<PumpToken>> = _tokens.asStateFlow()

    private val _marketFeedStatus = MutableStateFlow(MarketFeedStatus.CONNECTING)
    val marketFeedStatus: StateFlow<MarketFeedStatus> = _marketFeedStatus.asStateFlow()

    private val _lastMarketUpdate = MutableStateFlow<Long?>(null)
    val lastMarketUpdate: StateFlow<Long?> = _lastMarketUpdate.asStateFlow()

    private val _marketError = MutableStateFlow<String?>(null)
    val marketError: StateFlow<String?> = _marketError.asStateFlow()

    private val refreshMutex = Mutex()

    // This is a paper-trading profile only. No private key or on-chain wallet is connected.
    private val _walletSolBalance = MutableStateFlow(5.0)
    val walletSolBalance: StateFlow<Double> = _walletSolBalance.asStateFlow()

    private val _currentWallet = MutableStateFlow(
        SolanaWallet(address = "", label = "Paper Trading", isImported = false)
    )
    val currentWallet: StateFlow<SolanaWallet> = _currentWallet.asStateFlow()

    private val _holdings = MutableStateFlow<List<PortfolioHolding>>(emptyList())
    val holdings: StateFlow<List<PortfolioHolding>> = _holdings.asStateFlow()

    private val _limitOrders = MutableStateFlow<List<LimitOrder>>(emptyList())
    val limitOrders: StateFlow<List<LimitOrder>> = _limitOrders.asStateFlow()

    private val _transactions = MutableStateFlow<List<TradeTransaction>>(emptyList())
    val transactions: StateFlow<List<TradeTransaction>> = _transactions.asStateFlow()

    private val _alerts = MutableStateFlow<List<PriceAlert>>(emptyList())
    val alerts: StateFlow<List<PriceAlert>> = _alerts.asStateFlow()

    private val _botConfig = MutableStateFlow(BotStrategyConfig())
    val botConfig: StateFlow<BotStrategyConfig> = _botConfig.asStateFlow()

    private val _recentNotification = MutableStateFlow<PriceAlert?>(null)
    val recentNotification: StateFlow<PriceAlert?> = _recentNotification.asStateFlow()

    init {
        refreshMarketData()
        startMarketRefreshLoop()
    }

    fun clearRecentNotification() {
        _recentNotification.value = null
    }

    /**
     * Fetches real Solana DEX pair data. All trades remain local paper simulations.
     * A failed refresh preserves the last good snapshot and marks it stale.
     */
    fun refreshMarketData() {
        scope.launch {
            refreshMutex.withLock {
                _marketFeedStatus.value = if (_tokens.value.isEmpty()) {
                    MarketFeedStatus.CONNECTING
                } else {
                    _marketFeedStatus.value
                }
                try {
                    val snapshot = marketDataSource.fetchLatestSolanaMarket()
                    val mappedTokens = snapshot.tokens.map { pair ->
                        pair.toPumpToken(snapshot.solPriceUsd)
                    }
                    if (mappedTokens.isEmpty()) {
                        throw IllegalStateException("DexScreener returned no liquid Solana token pairs.")
                    }

                    val favorites = _tokens.value.filter { it.isFavorite }.map { it.id }.toSet()
                    _tokens.value = mappedTokens.map { it.copy(isFavorite = it.id in favorites) }
                    _solPriceUsd.value = snapshot.solPriceUsd
                    _lastMarketUpdate.value = System.currentTimeMillis()
                    _marketError.value = null
                    _marketFeedStatus.value = MarketFeedStatus.LIVE

                    // Limit orders are evaluated only against fresh market snapshots and remain paper-only.
                    _tokens.value.forEach { token ->
                        if (token.priceSol > 0.0) checkLimitOrders(token)
                    }
                } catch (error: Exception) {
                    _marketError.value = error.message ?: "Unable to refresh market data."
                    _marketFeedStatus.value = if (_tokens.value.isEmpty()) {
                        MarketFeedStatus.ERROR
                    } else {
                        MarketFeedStatus.STALE
                    }
                }
            }
        }
    }

    private fun startMarketRefreshLoop() {
        scope.launch {
            while (true) {
                delay(MARKET_REFRESH_INTERVAL_MS)
                refreshMarketData()
            }
        }
    }

    private fun DexScreenerMarketToken.toPumpToken(solUsd: Double): PumpToken {
        val safeSolUsd = solUsd.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val createdAt = pairCreatedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        return PumpToken(
            id = mintAddress,
            name = name,
            symbol = symbol,
            description = description,
            avatarEmoji = "🪙",
            avatarBgColor = 0xFF142033,
            createdAt = createdAt,
            priceUsd = priceUsd,
            priceSol = if (safeSolUsd > 0.0) priceUsd / safeSolUsd else 0.0,
            change1m = 0.0, // DexScreener does not publish a 1-minute change field.
            change5m = change5mPct,
            change1h = change1hPct,
            marketCapUsd = marketCapUsd,
            volume24hUsd = volume24hUsd,
            bondingCurveProgress = 0f,
            holderCount = 0, // Not included in the public pair response.
            telegramUrl = telegramUrl,
            twitterUrl = twitterUrl,
            websiteUrl = websiteUrl,
            dexPaidStatus = DexPaidStatus.PENDING, // Paid status is not inferred from boosts or pair presence.
            dexPaidTime = null,
            rugAnalysis = RugAnalysis(
                devHoldingPct = 0.0,
                top10HoldersPct = 0.0,
                isMintRenounced = false,
                isFreezeDisabled = false,
                isLpBurned = false,
                honeypotRisk = false,
                bundledSnipersCount = 0,
                warnings = listOf("Security checks are unverified. DexScreener pair data does not prove mint authority, holder distribution, LP lock, or honeypot status."),
                isVerified = false
            ),
            candles = emptyList(), // No synthetic candles: historical OHLC data is not fabricated.
            stage = TokenLaunchStage.DEX_LISTED,
            bondingCurveKnown = false,
            imageUrl = iconUrl,
            pairUrl = pairUrl
        )
    }

    private companion object {
        const val MARKET_REFRESH_INTERVAL_MS = 30_000L
    }

    private fun checkLimitOrders(token: PumpToken) {
        val config = _botConfig.value
        if (!config.isMasterEnabled) return

        val orders = _limitOrders.value.toMutableList()
        var updated = false

        orders.forEachIndexed { index, order ->
            if (order.tokenId == token.id && order.status == OrderStatus.ACTIVE) {
                val isTriggered = when (order.orderType) {
                    OrderType.BUY_LIMIT -> token.priceSol <= order.targetPriceSol
                    OrderType.SELL_LIMIT -> token.priceSol >= order.targetPriceSol
                    OrderType.TAKE_PROFIT -> token.priceSol >= order.targetPriceSol
                    OrderType.STOP_LOSS -> token.priceSol <= order.targetPriceSol
                }

                if (isTriggered) {
                    val executed = executeOrder(order, token)
                    if (executed) {
                        orders[index] = order.copy(
                            status = OrderStatus.FILLED,
                            filledAt = System.currentTimeMillis(),
                            executionNote = "Paper fill at " + String.format("%.7f", token.priceSol) + " SOL"
                        )
                        val botAlert = PriceAlert(
                            id = UUID.randomUUID().toString(),
                            tokenId = token.id,
                            tokenSymbol = token.symbol,
                            title = "Paper Order Filled: " + order.orderType.name,
                            message = "Paper simulation only. " + token.symbol + " reached " +
                                String.format("%.7f", token.priceSol) + " SOL. No blockchain transaction was sent.",
                            timestamp = System.currentTimeMillis(),
                            type = AlertType.BOT_TRIGGER
                        )
                        _alerts.value = listOf(botAlert) + _alerts.value
                        _recentNotification.value = botAlert
                    } else {
                        orders[index] = order.copy(
                            executionNote = "Trigger reached, but paper execution failed. Order remains active."
                        )
                    }
                    updated = true
                }
            }
        }

        if (updated) {
            _limitOrders.value = orders
        }
    }

    private fun executeOrder(order: LimitOrder, token: PumpToken): Boolean {
        return when (order.orderType) {
            OrderType.BUY_LIMIT -> buyToken(token.id, order.amountSol, isAutoBot = true)
            OrderType.SELL_LIMIT, OrderType.TAKE_PROFIT, OrderType.STOP_LOSS -> {
                val holding = _holdings.value.find { it.tokenId == token.id } ?: return false
                sellToken(token.id, holding.amountTokens, isAutoBot = true)
            }
        }
    }

    fun buyToken(tokenId: String, amountSol: Double, isAutoBot: Boolean = false): Boolean {
        if (!amountSol.isFinite() || amountSol <= 0.0 || _walletSolBalance.value < amountSol) return false
        val token = _tokens.value.find { it.id == tokenId } ?: return false
        if (!token.priceSol.isFinite() || token.priceSol <= 0.0) return false

        // Deduct SOL
        _walletSolBalance.value -= amountSol
        val tokensBought = amountSol / token.priceSol

        // Update holdings
        val currentHoldings = _holdings.value.toMutableList()
        val existingIndex = currentHoldings.indexOfFirst { it.tokenId == tokenId }
        if (existingIndex >= 0) {
            val existing = currentHoldings[existingIndex]
            val newAmount = existing.amountTokens + tokensBought
            val newTotalInvested = existing.totalInvestedSol + amountSol
            val newEntryPrice = newTotalInvested / newAmount
            currentHoldings[existingIndex] = existing.copy(
                amountTokens = newAmount,
                totalInvestedSol = newTotalInvested,
                entryPriceSol = newEntryPrice
            )
        } else {
            currentHoldings.add(
                PortfolioHolding(
                    tokenId = token.id,
                    tokenSymbol = token.symbol,
                    tokenName = token.name,
                    avatarEmoji = token.avatarEmoji,
                    amountTokens = tokensBought,
                    entryPriceSol = token.priceSol,
                    totalInvestedSol = amountSol
                )
            )
        }
        _holdings.value = currentHoldings

        // Record trade transaction
        val tx = TradeTransaction(
            id = UUID.randomUUID().toString(),
            tokenId = token.id,
            tokenSymbol = token.symbol,
            type = "BUY",
            amountSol = amountSol,
            amountTokens = tokensBought,
            priceSol = token.priceSol,
            realizedPnlSol = null,
            timestamp = System.currentTimeMillis(),
            txHash = "PAPER-${UUID.randomUUID().toString().take(12)}",
            isAutoBot = isAutoBot
        )
        _transactions.value = listOf(tx) + _transactions.value
        return true
    }

    fun sellToken(tokenId: String, amountTokensToSell: Double, isAutoBot: Boolean = false): Boolean {
        if (!amountTokensToSell.isFinite() || amountTokensToSell <= 0.0) return false
        val token = _tokens.value.find { it.id == tokenId } ?: return false
        if (!token.priceSol.isFinite() || token.priceSol <= 0.0) return false
        val currentHoldings = _holdings.value.toMutableList()
        val index = currentHoldings.indexOfFirst { it.tokenId == tokenId }
        if (index < 0) return false

        val holding = currentHoldings[index]
        val actualSellAmount = amountTokensToSell.coerceAtMost(holding.amountTokens)
        val solReceived = actualSellAmount * token.priceSol
        val costBasis = actualSellAmount * holding.entryPriceSol
        val realizedPnl = solReceived - costBasis

        // Add SOL to wallet
        _walletSolBalance.value += solReceived

        if (actualSellAmount >= holding.amountTokens - 0.0001) {
            currentHoldings.removeAt(index)
        } else {
            val remaining = holding.amountTokens - actualSellAmount
            val remainingInvested = remaining * holding.entryPriceSol
            currentHoldings[index] = holding.copy(
                amountTokens = remaining,
                totalInvestedSol = remainingInvested
            )
        }
        _holdings.value = currentHoldings

        // Record trade transaction
        val tx = TradeTransaction(
            id = UUID.randomUUID().toString(),
            tokenId = token.id,
            tokenSymbol = token.symbol,
            type = "SELL",
            amountSol = solReceived,
            amountTokens = actualSellAmount,
            priceSol = token.priceSol,
            realizedPnlSol = realizedPnl,
            timestamp = System.currentTimeMillis(),
            txHash = "PAPER-${UUID.randomUUID().toString().take(12)}",
            isAutoBot = isAutoBot
        )
        _transactions.value = listOf(tx) + _transactions.value
        return true
    }

    fun addLimitOrder(
        tokenId: String,
        orderType: OrderType,
        targetPriceSol: Double,
        amountSol: Double,
        slippagePct: Double = 5.0
    ) {
        val token = _tokens.value.find { it.id == tokenId } ?: return
        val condition = when (orderType) {
            OrderType.BUY_LIMIT, OrderType.STOP_LOSS -> "<="
            OrderType.SELL_LIMIT, OrderType.TAKE_PROFIT -> ">="
        }
        val order = LimitOrder(
            id = UUID.randomUUID().toString(),
            tokenId = token.id,
            tokenSymbol = token.symbol,
            tokenName = token.name,
            orderType = orderType,
            targetPriceSol = targetPriceSol,
            triggerCondition = condition,
            amountSol = amountSol,
            slippagePct = slippagePct,
            status = OrderStatus.ACTIVE
        )
        _limitOrders.value = listOf(order) + _limitOrders.value
    }

    fun cancelLimitOrder(orderId: String) {
        _limitOrders.value = _limitOrders.value.map {
            if (it.id == orderId) it.copy(status = OrderStatus.CANCELLED) else it
        }
    }

    fun toggleFavorite(tokenId: String) {
        _tokens.value = _tokens.value.map {
            if (it.id == tokenId) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    fun updateBotConfig(config: BotStrategyConfig) {
        _botConfig.value = config
    }

    fun resetWalletBalance(amountSol: Double = 5.0) {
        _walletSolBalance.value = amountSol
    }

    fun importPrivateKey(rawKey: String, customLabel: String = "Imported Wallet"): Result<SolanaWallet> {
        // Import is intentionally disabled until audited Ed25519 derivation, secure key storage,
        // and transaction signing are integrated. Never derive a fake public key from user input.
        return Result.failure(
            UnsupportedOperationException(
                "Live wallet import is not available yet. The current app is in Paper Trading mode; do not enter a seed phrase or private key."
            )
        )
    }

    fun disconnectWallet() {
        _currentWallet.value = SolanaWallet(
            address = "",
            label = "Paper Trading",
            isImported = false
        )
        val alert = PriceAlert(
            id = UUID.randomUUID().toString(),
            tokenId = "",
            tokenSymbol = "SOL",
            title = "Paper profile active",
            message = "No on-chain wallet is connected.",
            timestamp = System.currentTimeMillis(),
            type = AlertType.WALLET_EVENT
        )
        _alerts.value = listOf(alert) + _alerts.value
        _recentNotification.value = alert
    }

    fun withdrawSol(recipientAddress: String, amountSol: Double): Result<TradeTransaction> {
        // Prevent a simulated balance mutation from being mistaken for an on-chain transfer.
        return Result.failure(
            UnsupportedOperationException(
                "SOL withdrawals are disabled in this build. No real Solana transaction has been created or sent."
            )
        )
    }
}
