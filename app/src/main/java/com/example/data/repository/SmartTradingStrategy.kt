package com.example.data.repository

import com.example.data.model.PumpToken
import com.example.data.model.RiskLevel
import kotlin.math.ln

data class CandidateAssessment(
    val tokenId: String,
    val symbol: String,
    val score: Int,
    val eligible: Boolean,
    val reasons: List<String>,
    val blockers: List<String>
)

data class AutoExitDecision(
    val shouldSell: Boolean,
    val reason: String,
    val pnlPct: Double,
    val peakPnlPct: Double
)

/**
 * Deterministic screening and exit rules. A high score is a ranking heuristic,
 * not a forecast and never a promise of 30x returns.
 */
object SmartTradingStrategy {
    fun assess(token: PumpToken, config: com.example.data.model.BotStrategyConfig, nowMs: Long = System.currentTimeMillis()): CandidateAssessment {
        val reasons = mutableListOf<String>()
        val blockers = mutableListOf<String>()
        val ageHours = ((nowMs - token.createdAt).coerceAtLeast(0L) / 3_600_000.0)
        val liquidity = token.liquidityUsd.coerceAtLeast(0.0)
        val volume = token.volume24hUsd.coerceAtLeast(0.0)
        val marketCap = token.marketCapUsd.coerceAtLeast(0.0)

        // Normalize each factor to 0..1, then apply editable relative weights.
        val liquidityFactor = (ln(1.0 + liquidity / 1_000.0) / ln(1.0 + 100.0)).coerceIn(0.0, 1.0)
        val volumeFactor = (ln(1.0 + volume / 1_000.0) / ln(1.0 + 100.0)).coerceIn(0.0, 1.0)
        val momentumFactor = ((token.change5m.coerceIn(0.0, 20.0) / 20.0) * 0.60 +
            (token.change1h.coerceIn(0.0, 50.0) / 50.0) * 0.40).coerceIn(0.0, 1.0)
        val ageFactor = when {
            ageHours <= 1.0 -> 1.0
            ageHours <= 6.0 -> 0.867
            ageHours <= 24.0 -> 0.667
            ageHours <= config.maxTokenAgeHours -> 0.40
            else -> 0.0
        }
        val capFactor = when {
            marketCap in config.minMarketCapUsd..250_000.0 -> 1.0
            marketCap > 250_000.0 && marketCap <= config.maxMarketCapUsd -> 0.60
            else -> 0.0
        }
        val trendFactor = if (token.change5m > 0.0 && token.change1h > 0.0) 1.0 else 0.0
        val weights = listOf(config.scoreLiquidityWeight, config.scoreVolumeWeight,
            config.scoreMomentumWeight, config.scoreAgeWeight, config.scoreMarketCapWeight,
            config.scoreTrendWeight).map { it.coerceIn(0, 100) }
        val totalWeight = weights.sum()
        val weightedScore = liquidityFactor * weights[0] + volumeFactor * weights[1] +
            momentumFactor * weights[2] + ageFactor * weights[3] + capFactor * weights[4] +
            trendFactor * weights[5]
        val score = if (totalWeight > 0) (weightedScore / totalWeight * 100.0).toInt().coerceIn(0, 100) else 0

        if (liquidity < config.minLiquidityUsd) blockers += "Liquidity below minimum" else reasons += "Liquidity meets minimum"
        if (volume < config.minVolume24hUsd) blockers += "24h volume below minimum" else reasons += "24h volume meets minimum"
        if (marketCap !in config.minMarketCapUsd..config.maxMarketCapUsd) blockers += "Market cap outside configured range" else reasons += "Market cap within configured range"
        if (ageHours > config.maxTokenAgeHours) blockers += "Token pair is older than configured maximum" else reasons += "Pair age within configured range"
        if (token.change5m <= 0.0 || token.change1h <= 0.0) blockers += "Positive 5m and 1h momentum required" else reasons += "Positive 5m and 1h momentum"
        if (!token.priceSol.isFinite() || token.priceSol <= 0.0) blockers += "Invalid token price"
        if (!token.rugAnalysis.isVerified && !config.allowUnverifiedSecurityData) blockers += "On-chain security checks are unverified"
        if (token.rugAnalysis.isVerified && token.rugAnalysis.riskLevel == RiskLevel.HIGH_RISK) blockers += "Token security risk is high"
        if (score < config.minCandidateScore) blockers += "Candidate score below threshold"

        return CandidateAssessment(token.id, token.symbol, score, blockers.isEmpty(), reasons, blockers)
    }

    fun evaluateExit(
        pnlPct: Double,
        peakPnlPct: Double,
        takeProfitPct: Double,
        stopLossPct: Double,
        trailingStopPct: Double
    ): AutoExitDecision {
        val safePeak = maxOf(peakPnlPct, pnlPct)
        return when {
            pnlPct <= -stopLossPct -> AutoExitDecision(true, "STOP_LOSS", pnlPct, safePeak)
            pnlPct >= takeProfitPct -> AutoExitDecision(true, "TAKE_PROFIT", pnlPct, safePeak)
            safePeak > 0.0 && pnlPct <= safePeak - trailingStopPct ->
                AutoExitDecision(true, "TRAILING_STOP", pnlPct, safePeak)
            else -> AutoExitDecision(false, "HOLD", pnlPct, safePeak)
        }
    }
}