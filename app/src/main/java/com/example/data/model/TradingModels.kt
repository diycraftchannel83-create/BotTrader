package com.example.data.model

enum class OrderType {
    BUY_LIMIT,
    SELL_LIMIT,
    TAKE_PROFIT,
    STOP_LOSS
}

enum class OrderStatus {
    ACTIVE,
    TRIGGERED,
    FILLED,
    CANCELLED
}

data class LimitOrder(
    val id: String,
    val tokenId: String,
    val tokenSymbol: String,
    val tokenName: String,
    val orderType: OrderType,
    val targetPriceSol: Double,
    val triggerCondition: String, // e.g. "<=" or ">="
    val amountSol: Double,
    val slippagePct: Double = 5.0,
    val status: OrderStatus = OrderStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val filledAt: Long? = null,
    val executionNote: String = ""
)

data class PortfolioHolding(
    val tokenId: String,
    val tokenSymbol: String,
    val tokenName: String,
    val avatarEmoji: String,
    val amountTokens: Double,
    val entryPriceSol: Double,
    val totalInvestedSol: Double
) {
    fun currentValSol(currentPriceSol: Double): Double = amountTokens * currentPriceSol
    fun pnlSol(currentPriceSol: Double): Double = currentValSol(currentPriceSol) - totalInvestedSol
    fun pnlPct(currentPriceSol: Double): Double =
        if (totalInvestedSol > 0) ((currentValSol(currentPriceSol) - totalInvestedSol) / totalInvestedSol) * 100.0 else 0.0
}

data class SolanaWallet(
    val address: String,
    val label: String,
    val isImported: Boolean,
    val importedAt: Long? = null,
    val maskedKey: String? = null
) {
    val shortAddress: String
        get() = if (address.length > 8) "${address.take(4)}...${address.takeLast(4)}" else address
}

data class TradeTransaction(
    val id: String,
    val tokenId: String,
    val tokenSymbol: String,
    val type: String, // "BUY", "SELL", "WITHDRAW"
    val amountSol: Double,
    val amountTokens: Double,
    val priceSol: Double,
    val realizedPnlSol: Double?,
    val timestamp: Long,
    val txHash: String,
    val isAutoBot: Boolean,
    val recipientAddress: String? = null
)

enum class AlertType {
    PRICE_SURGE,
    PRICE_DUMP,
    DEX_PAID,
    GRADUATION,
    RUG_WARNING,
    BOT_TRIGGER,
    WITHDRAWAL,
    WALLET_EVENT
}

data class PriceAlert(
    val id: String,
    val tokenId: String,
    val tokenSymbol: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: AlertType
)

data class BotStrategyConfig(
    val isMasterEnabled: Boolean = false,
    val autoSnipeNewTokens: Boolean = false,
    val minRugScoreForSnipe: Int = 85,
    val maxDevHoldingForSnipe: Double = 5.0,
    val requireDexPaidForSnipe: Boolean = true,
    val snipeAmountSol: Double = 0.25,
    val defaultTakeProfitPct: Double = 75.0,
    val defaultStopLossPct: Double = 25.0,
    val trailingStopEnabled: Boolean = true,
    val trailingStopPct: Double = 15.0,
    val defaultSlippagePct: Double = 10.0,
    val maxPriorityFeeSol: Double = 0.005,
    val autoBuyEnabled: Boolean = false,
    val autoSellEnabled: Boolean = true,
    val buyAmountSol: Double = 0.01,
    val minCandidateScore: Int = 75,
    val minLiquidityUsd: Double = 20_000.0,
    val minVolume24hUsd: Double = 50_000.0,
    val minMarketCapUsd: Double = 10_000.0,
    val maxMarketCapUsd: Double = 2_000_000.0,
    val maxTokenAgeHours: Double = 48.0,
    val maxOpenPositions: Int = 3,
    val maxDailyLossSol: Double = 0.05,
    val takeProfitPct: Double = 100.0,
    val stopLossPct: Double = 20.0,
    val trailingStopPctAuto: Double = 15.0,
    val allowUnverifiedSecurityData: Boolean = false
)
