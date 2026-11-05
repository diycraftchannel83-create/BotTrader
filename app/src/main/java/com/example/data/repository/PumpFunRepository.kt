package com.example.data.repository

import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

class PumpFunRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    val solPriceUsd = 185.50

    private val _tokens = MutableStateFlow<List<PumpToken>>(emptyList())
    val tokens: StateFlow<List<PumpToken>> = _tokens.asStateFlow()

    private val _walletSolBalance = MutableStateFlow(5.0)
    val walletSolBalance: StateFlow<Double> = _walletSolBalance.asStateFlow()

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
        initializeTokens()
        startLiveMarketEngine()
    }

    fun clearRecentNotification() {
        _recentNotification.value = null
    }

    private fun initializeTokens() {
        val initialTokens = listOf(
            createInitialToken(
                name = "Pepe In Space",
                symbol = "PEPESPACE",
                desc = "The first frog on Mars with autonomous laser propulsion and community moonshot.",
                emoji = "🐸",
                color = 0xFF103B2B,
                priceSol = 0.0000452,
                bondingProgress = 0.82f,
                holders = 412,
                dexPaid = DexPaidStatus.CONFIRMED,
                tg = "https://t.me/pepespacesol",
                x = "https://x.com/pepespacesol",
                web = "https://pepespace.fun",
                rugAnalysis = RugAnalysis(
                    devHoldingPct = 2.1,
                    top10HoldersPct = 21.5,
                    isMintRenounced = true,
                    isFreezeDisabled = true,
                    isLpBurned = true,
                    honeypotRisk = false,
                    bundledSnipersCount = 1,
                    warnings = emptyList()
                ),
                stage = TokenLaunchStage.BONDING
            ),
            createInitialToken(
                name = "Quantum Doge AI",
                symbol = "QDOGE",
                desc = "AI-powered algorithmic meme dog generating continuous liquidity reflections.",
                emoji = "🐕",
                color = 0xFF3B2A10,
                priceSol = 0.0000891,
                bondingProgress = 0.94f,
                holders = 789,
                dexPaid = DexPaidStatus.CONFIRMED,
                tg = "https://t.me/quantumdogeai",
                x = "https://x.com/quantumdoge",
                web = "https://qdoge.tech",
                rugAnalysis = RugAnalysis(
                    devHoldingPct = 3.5,
                    top10HoldersPct = 27.0,
                    isMintRenounced = true,
                    isFreezeDisabled = true,
                    isLpBurned = true,
                    honeypotRisk = false,
                    bundledSnipersCount = 2,
                    warnings = listOf("Dev sold 1% 10m ago")
                ),
                stage = TokenLaunchStage.BONDING
            ),
            createInitialToken(
                name = "Rugproof Cat",
                symbol = "RCAT",
                desc = "Anti-drainer meme cat with locked LP and community council voting.",
                emoji = "🐱",
                color = 0xFF2B103B,
                priceSol = 0.0000210,
                bondingProgress = 0.44f,
                holders = 230,
                dexPaid = DexPaidStatus.CONFIRMED,
                tg = "https://t.me/rugproofcat",
                x = "https://x.com/rugproofcat",
                web = "https://rugproofcat.xyz",
                rugAnalysis = RugAnalysis(
                    devHoldingPct = 1.8,
                    top10HoldersPct = 19.2,
                    isMintRenounced = true,
                    isFreezeDisabled = true,
                    isLpBurned = true,
                    honeypotRisk = false,
                    bundledSnipersCount = 0,
                    warnings = emptyList()
                ),
                stage = TokenLaunchStage.BONDING
            ),
            createInitialToken(
                name = "Solana Capybara",
                symbol = "CAPY",
                desc = "Chillest capybara in crypto taking baths in pump.fun liquidity pools.",
                emoji = "🦫",
                color = 0xFF2D251A,
                priceSol = 0.000142,
                bondingProgress = 1.0f,
                holders = 1840,
                dexPaid = DexPaidStatus.CONFIRMED,
                tg = "https://t.me/solcapy",
                x = "https://x.com/solcapy",
                web = "https://capybarasol.org",
                rugAnalysis = RugAnalysis(
                    devHoldingPct = 0.0,
                    top10HoldersPct = 14.5,
                    isMintRenounced = true,
                    isFreezeDisabled = true,
                    isLpBurned = true,
                    honeypotRisk = false,
                    bundledSnipersCount = 0,
                    warnings = emptyList()
                ),
                stage = TokenLaunchStage.GRADUATED
            ),
            createInitialToken(
                name = "Moon Rocket Retardio",
                symbol = "MRET",
                desc = "Ultra degeneracy rocket targeting 100x or zero. Fasten seatbelts.",
                emoji = "🚀",
                color = 0xFF351212,
                priceSol = 0.0000155,
                bondingProgress = 0.18f,
                holders = 95,
                dexPaid = DexPaidStatus.PENDING,
                tg = "https://t.me/moonrocketret",
                x = "https://x.com/moonrocketret",
                web = "https://moonretardio.fun",
                rugAnalysis = RugAnalysis(
                    devHoldingPct = 12.5,
                    top10HoldersPct = 52.0,
                    isMintRenounced = true,
                    isFreezeDisabled = false,
                    isLpBurned = false,
                    honeypotRisk = false,
                    bundledSnipersCount = 8,
                    warnings = listOf("High Dev Holding (12.5%)", "Freeze authority still active", "Top 10 holds >50%")
                ),
                stage = TokenLaunchStage.NEW_BORN
            ),
            createInitialToken(
                name = "Gigachad Banana",
                symbol = "GIGABAN",
                desc = "Potassium-infused alpha meme coin built for high leverage traders.",
                emoji = "🍌",
                color = 0xFF363211,
                priceSol = 0.0000620,
                bondingProgress = 0.63f,
                holders = 360,
                dexPaid = DexPaidStatus.CONFIRMED,
                tg = "https://t.me/gigabanana",
                x = "https://x.com/gigabanana",
                web = "https://gigabanana.vip",
                rugAnalysis = RugAnalysis(
                    devHoldingPct = 3.0,
                    top10HoldersPct = 25.4,
                    isMintRenounced = true,
                    isFreezeDisabled = true,
                    isLpBurned = true,
                    honeypotRisk = false,
                    bundledSnipersCount = 3,
                    warnings = emptyList()
                ),
                stage = TokenLaunchStage.BONDING
            ),
            createInitialToken(
                name = "ScamInu 9000",
                symbol = "SCAM",
                desc = "Suspicious new token flagged by automated security audits. Extreme danger.",
                emoji = "⚠️",
                color = 0xFF381010,
                priceSol = 0.0000078,
                bondingProgress = 0.08f,
                holders = 32,
                dexPaid = DexPaidStatus.UNPAID,
                tg = "https://t.me/scaminu",
                x = "https://x.com/scaminu",
                web = "",
                rugAnalysis = RugAnalysis(
                    devHoldingPct = 34.0,
                    top10HoldersPct = 78.0,
                    isMintRenounced = false,
                    isFreezeDisabled = false,
                    isLpBurned = false,
                    honeypotRisk = true,
                    bundledSnipersCount = 14,
                    warnings = listOf("CRITICAL: Mint authority NOT renounced", "Dev owns 34% of supply", "High honeypot transfer tax detected", "14 bundled snipers in block 0")
                ),
                stage = TokenLaunchStage.NEW_BORN
            )
        )
        _tokens.value = initialTokens

        // Give user an initial holding to test portfolio analytics immediately
        val initialHoldingToken = initialTokens[0]
        val holdingAmount = 15000.0
        val entryPrice = 0.0000350
        _holdings.value = listOf(
            PortfolioHolding(
                tokenId = initialHoldingToken.id,
                tokenSymbol = initialHoldingToken.symbol,
                tokenName = initialHoldingToken.name,
                avatarEmoji = initialHoldingToken.avatarEmoji,
                amountTokens = holdingAmount,
                entryPriceSol = entryPrice,
                totalInvestedSol = holdingAmount * entryPrice
            )
        )

        // Seed initial limit order
        _limitOrders.value = listOf(
            LimitOrder(
                id = UUID.randomUUID().toString(),
                tokenId = initialHoldingToken.id,
                tokenSymbol = initialHoldingToken.symbol,
                tokenName = initialHoldingToken.name,
                orderType = OrderType.TAKE_PROFIT,
                targetPriceSol = 0.0000750,
                triggerCondition = ">=",
                amountSol = 0.5,
                slippagePct = 5.0,
                status = OrderStatus.ACTIVE
            ),
            LimitOrder(
                id = UUID.randomUUID().toString(),
                tokenId = initialTokens[2].id,
                tokenSymbol = initialTokens[2].symbol,
                tokenName = initialTokens[2].name,
                orderType = OrderType.BUY_LIMIT,
                targetPriceSol = 0.0000170,
                triggerCondition = "<=",
                amountSol = 0.25,
                slippagePct = 8.0,
                status = OrderStatus.ACTIVE
            )
        )

        _alerts.value = listOf(
            PriceAlert(
                id = UUID.randomUUID().toString(),
                tokenId = initialTokens[0].id,
                tokenSymbol = initialTokens[0].symbol,
                title = "🚀 Dex Paid Confirmed!",
                message = "${initialTokens[0].symbol} fast-track listing fee verified on DexScreener.",
                timestamp = System.currentTimeMillis() - 120_000,
                type = AlertType.DEX_PAID
            ),
            PriceAlert(
                id = UUID.randomUUID().toString(),
                tokenId = initialTokens[1].id,
                tokenSymbol = initialTokens[1].symbol,
                title = "🔥 Price Surge +24.8% in 1m",
                message = "${initialTokens[1].symbol} massive buy of 4.2 SOL by whale detected!",
                timestamp = System.currentTimeMillis() - 60_000,
                type = AlertType.PRICE_SURGE
            )
        )
    }

    private fun createInitialToken(
        name: String,
        symbol: String,
        desc: String,
        emoji: String,
        color: Long,
        priceSol: Double,
        bondingProgress: Float,
        holders: Int,
        dexPaid: DexPaidStatus,
        tg: String,
        x: String,
        web: String,
        rugAnalysis: RugAnalysis,
        stage: TokenLaunchStage
    ): PumpToken {
        val now = System.currentTimeMillis()
        val candles = generateMockCandles(priceSol, 25)
        return PumpToken(
            id = "pump_${symbol.lowercase()}_${Random.nextInt(1000, 9999)}",
            name = name,
            symbol = symbol,
            description = desc,
            avatarEmoji = emoji,
            avatarBgColor = color,
            createdAt = now - Random.nextLong(600_000, 7_200_000),
            priceUsd = priceSol * solPriceUsd,
            priceSol = priceSol,
            change1m = Random.nextDouble(-3.0, 7.5),
            change5m = Random.nextDouble(-8.0, 18.0),
            change1h = Random.nextDouble(-12.0, 45.0),
            marketCapUsd = (priceSol * solPriceUsd * 1_000_000_000) * (0.00000003 + bondingProgress * 0.0000008),
            volume24hUsd = Random.nextDouble(5_000.0, 85_000.0),
            bondingCurveProgress = bondingProgress,
            holderCount = holders,
            telegramUrl = tg,
            twitterUrl = x,
            websiteUrl = web,
            dexPaidStatus = dexPaid,
            dexPaidTime = if (dexPaid == DexPaidStatus.CONFIRMED) now - 300_000 else null,
            rugAnalysis = rugAnalysis,
            candles = candles,
            stage = stage
        )
    }

    private fun generateMockCandles(currentPrice: Double, count: Int): List<PriceCandle> {
        val list = mutableListOf<PriceCandle>()
        var base = currentPrice * 0.65
        val now = System.currentTimeMillis()
        val interval = 60_000L

        for (i in 0 until count) {
            val variance = (Random.nextDouble() - 0.46) * (base * 0.12)
            val open = base
            val close = (base + variance).coerceAtLeast(0.000001)
            val high = maxOf(open, close) * (1.0 + Random.nextDouble(0.01, 0.05))
            val low = minOf(open, close) * (1.0 - Random.nextDouble(0.01, 0.05))
            val vol = Random.nextDouble(100.0, 3000.0)
            list.add(PriceCandle(now - (count - i) * interval, open, high, low, close, vol))
            base = close
        }
        return list
    }

    private fun startLiveMarketEngine() {
        scope.launch {
            while (true) {
                delay(1800) // tick every 1.8 seconds for smooth trading feedback
                tickMarket()
            }
        }
    }

    private fun tickMarket() {
        val currentTokens = _tokens.value.toMutableList()
        val updatedTokens = mutableListOf<PumpToken>()
        val newAlerts = mutableListOf<PriceAlert>()

        currentTokens.forEach { token ->
            // Simulate random market volatility
            val percentChange = (Random.nextDouble() - 0.48) * 8.5
            val newPriceSol = (token.priceSol * (1.0 + percentChange / 100.0)).coerceAtLeast(0.0000001)
            val newPriceUsd = newPriceSol * solPriceUsd

            val updatedChange1m = token.change1m * 0.7 + percentChange * 0.3
            val updatedChange5m = token.change5m + percentChange * 0.15

            // Progress bonding curve slightly on buys
            var newProgress = token.bondingCurveProgress
            if (percentChange > 0 && newProgress < 1.0f) {
                newProgress = (newProgress + (percentChange.toFloat() * 0.0015f)).coerceAtMost(1.0f)
            }
            val newStage = if (newProgress >= 1.0f) TokenLaunchStage.GRADUATED else token.stage

            // Update candles
            val lastCandle = token.candles.lastOrNull()
            val updatedCandles = token.candles.toMutableList()
            if (lastCandle != null) {
                val newHigh = maxOf(lastCandle.high, newPriceSol)
                val newLow = minOf(lastCandle.low, newPriceSol)
                val newVol = lastCandle.volume + (if (percentChange > 0) percentChange * 50 else 20.0)
                updatedCandles[updatedCandles.lastIndex] = lastCandle.copy(
                    high = newHigh,
                    low = newLow,
                    close = newPriceSol,
                    volume = newVol
                )
            }

            // Significant price movement alert (> 12% jump or drop in 1 tick/burst)
            if (percentChange >= 12.0) {
                val alert = PriceAlert(
                    id = UUID.randomUUID().toString(),
                    tokenId = token.id,
                    tokenSymbol = token.symbol,
                    title = "🚀 Spike Alert: ${token.symbol} +${String.format("%.1f", percentChange)}%",
                    message = "${token.name} breaking resistance! Volume surging on pump.fun.",
                    timestamp = System.currentTimeMillis(),
                    type = AlertType.PRICE_SURGE
                )
                newAlerts.add(alert)
                _recentNotification.value = alert
            } else if (percentChange <= -14.0) {
                val alert = PriceAlert(
                    id = UUID.randomUUID().toString(),
                    tokenId = token.id,
                    tokenSymbol = token.symbol,
                    title = "🔻 Dump Alert: ${token.symbol} ${String.format("%.1f", percentChange)}%",
                    message = "${token.name} experiencing heavy selloff.",
                    timestamp = System.currentTimeMillis(),
                    type = AlertType.PRICE_DUMP
                )
                newAlerts.add(alert)
                _recentNotification.value = alert
            }

            // Graduation alert
            if (token.stage != TokenLaunchStage.GRADUATED && newStage == TokenLaunchStage.GRADUATED) {
                val alert = PriceAlert(
                    id = UUID.randomUUID().toString(),
                    tokenId = token.id,
                    tokenSymbol = token.symbol,
                    title = "🎓 Raydium Graduation!",
                    message = "${token.name} has hit 100% bonding curve and migrated to Raydium DEX!",
                    timestamp = System.currentTimeMillis(),
                    type = AlertType.GRADUATION
                )
                newAlerts.add(alert)
                _recentNotification.value = alert
            }

            val updatedToken = token.copy(
                priceSol = newPriceSol,
                priceUsd = newPriceUsd,
                change1m = updatedChange1m,
                change5m = updatedChange5m,
                bondingCurveProgress = newProgress,
                stage = newStage,
                candles = updatedCandles
            )
            updatedTokens.add(updatedToken)

            // Check automated limit orders for this token!
            checkLimitOrders(updatedToken)
        }

        if (newAlerts.isNotEmpty()) {
            _alerts.value = (newAlerts + _alerts.value).take(50)
        }
        _tokens.value = updatedTokens
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
                    // Execute order via automated trading bot
                    executeOrder(order, token)
                    orders[index] = order.copy(
                        status = OrderStatus.FILLED,
                        filledAt = System.currentTimeMillis(),
                        executionNote = "Bot executed at ${String.format("%.7f", token.priceSol)} SOL"
                    )
                    updated = true

                    val botAlert = PriceAlert(
                        id = UUID.randomUUID().toString(),
                        tokenId = token.id,
                        tokenSymbol = token.symbol,
                        title = "🤖 Bot Trade Executed: ${order.orderType.name}",
                        message = "Target reached for ${token.symbol} at ${String.format("%.7f", token.priceSol)} SOL. Amount: ${order.amountSol} SOL",
                        timestamp = System.currentTimeMillis(),
                        type = AlertType.BOT_TRIGGER
                    )
                    _alerts.value = listOf(botAlert) + _alerts.value
                    _recentNotification.value = botAlert
                }
            }
        }

        if (updated) {
            _limitOrders.value = orders
        }
    }

    private fun executeOrder(order: LimitOrder, token: PumpToken) {
        when (order.orderType) {
            OrderType.BUY_LIMIT -> {
                buyToken(token.id, order.amountSol, isAutoBot = true)
            }
            OrderType.SELL_LIMIT, OrderType.TAKE_PROFIT, OrderType.STOP_LOSS -> {
                // Sell holdings for this token
                val holding = _holdings.value.find { it.tokenId == token.id }
                if (holding != null) {
                    // Sell all or proportional
                    sellToken(token.id, holding.amountTokens, isAutoBot = true)
                }
            }
        }
    }

    fun buyToken(tokenId: String, amountSol: Double, isAutoBot: Boolean = false): Boolean {
        if (_walletSolBalance.value < amountSol) return false
        val token = _tokens.value.find { it.id == tokenId } ?: return false

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
            txHash = "sol_tx_${UUID.randomUUID().toString().take(12)}",
            isAutoBot = isAutoBot
        )
        _transactions.value = listOf(tx) + _transactions.value
        return true
    }

    fun sellToken(tokenId: String, amountTokensToSell: Double, isAutoBot: Boolean = false): Boolean {
        val token = _tokens.value.find { it.id == tokenId } ?: return false
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
            txHash = "sol_tx_${UUID.randomUUID().toString().take(12)}",
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

    fun simulateNewTokenLaunch() {
        val memeNames = listOf(
            Pair("Cyber Samurai Inu", "SAMU") to "🥷",
            Pair("Trump Whale 2026", "TRUMPW") to "🇺🇸",
            Pair("Neon Hamster AI", "HAMMY") to "🐹",
            Pair("Solana Super Sonic", "SONIC") to "⚡",
            Pair("Fart Liquidity Hub", "FARTHUB") to "💨",
            Pair("Chill Bro Protocol", "CHILLBRO") to "😎"
        )
        val selected = memeNames.random()
        val hasDexPaid = Random.nextBoolean()
        val isRug = Random.nextDouble() < 0.25

        val devPct = if (isRug) Random.nextDouble(15.0, 35.0) else Random.nextDouble(1.0, 4.5)
        val top10 = if (isRug) Random.nextDouble(55.0, 85.0) else Random.nextDouble(15.0, 30.0)
        val mintRenounced = !isRug
        val freezeDisabled = !isRug
        val warnings = if (isRug) listOf("High dev ownership ($devPct%)", "Honeypot risk detected", "Mint authority alive") else emptyList()

        val token = createInitialToken(
            name = selected.first.first,
            symbol = selected.first.second,
            desc = "Freshly minted meme token live on pump.fun bonding curve.",
            emoji = selected.second,
            color = 0xFF142033,
            priceSol = 0.0000085,
            bondingProgress = 0.03f,
            holders = Random.nextInt(12, 45),
            dexPaid = if (hasDexPaid) DexPaidStatus.CONFIRMED else DexPaidStatus.UNPAID,
            tg = "https://t.me/${selected.first.second.lowercase()}",
            x = "https://x.com/${selected.first.second.lowercase()}",
            web = "https://${selected.first.second.lowercase()}.fun",
            rugAnalysis = RugAnalysis(
                devHoldingPct = devPct,
                top10HoldersPct = top10,
                isMintRenounced = mintRenounced,
                isFreezeDisabled = freezeDisabled,
                isLpBurned = !isRug,
                honeypotRisk = isRug,
                bundledSnipersCount = if (isRug) 9 else 0,
                warnings = warnings
            ),
            stage = TokenLaunchStage.NEW_BORN
        )

        _tokens.value = listOf(token) + _tokens.value

        val alert = PriceAlert(
            id = UUID.randomUUID().toString(),
            tokenId = token.id,
            tokenSymbol = token.symbol,
            title = if (hasDexPaid) "🚀 New Launch (Dex Paid!): ${token.symbol}" else "⚡ New Token Launch: ${token.symbol}",
            message = "${token.name} just minted on pump.fun! ${if (hasDexPaid) "DexScreener paid badge active!" else ""}",
            timestamp = System.currentTimeMillis(),
            type = if (hasDexPaid) AlertType.DEX_PAID else AlertType.PRICE_SURGE
        )
        _alerts.value = listOf(alert) + _alerts.value
        _recentNotification.value = alert

        // Auto-snipe check if enabled
        val config = _botConfig.value
        if (config.isMasterEnabled && config.autoSnipeNewTokens) {
            val passRug = token.rugAnalysis.safetyScore >= config.minRugScoreForSnipe
            val passDev = token.rugAnalysis.devHoldingPct <= config.maxDevHoldingForSnipe
            val passDex = !config.requireDexPaidForSnipe || token.dexPaidStatus == DexPaidStatus.CONFIRMED

            if (passRug && passDev && passDex && _walletSolBalance.value >= config.snipeAmountSol) {
                buyToken(token.id, config.snipeAmountSol, isAutoBot = true)
                val snipeAlert = PriceAlert(
                    id = UUID.randomUUID().toString(),
                    tokenId = token.id,
                    tokenSymbol = token.symbol,
                    title = "🎯 Auto-Snipe Executed: ${token.symbol}",
                    message = "Bot sniped ${config.snipeAmountSol} SOL on ${token.symbol} matching safety criteria (Score: ${token.rugAnalysis.safetyScore})",
                    timestamp = System.currentTimeMillis(),
                    type = AlertType.BOT_TRIGGER
                )
                _alerts.value = listOf(snipeAlert) + _alerts.value
                _recentNotification.value = snipeAlert
            }
        }
    }
}
