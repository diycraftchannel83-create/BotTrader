package com.example.data.model

enum class DexPaidStatus {
    CONFIRMED, // Dex Screener Fast-track paid
    PENDING,   // Processing or checking
    UNPAID     // Standard organic launch
}

enum class TokenLaunchStage {
    NEW_BORN,   // Just launched (< 15 mins)
    BONDING,    // Active bonding curve (progress 15% - 99%)
    GRADUATED,  // Verified graduation/migration from a bonding curve
    DEX_LISTED  // A live DEX pair exists; bonding-curve state is not implied
}

data class PriceCandle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class RugAnalysis(
    val devHoldingPct: Double,
    val top10HoldersPct: Double,
    val isMintRenounced: Boolean,
    val isFreezeDisabled: Boolean,
    val isLpBurned: Boolean,
    val honeypotRisk: Boolean,
    val bundledSnipersCount: Int,
    val warnings: List<String>,
    val isVerified: Boolean = true
) {
    val safetyScore: Int
        get() {
            if (!isVerified) return 0
            var score = 100
            if (devHoldingPct > 15.0) score -= 30
            else if (devHoldingPct > 8.0) score -= 15
            else if (devHoldingPct > 4.0) score -= 5

            if (top10HoldersPct > 45.0) score -= 25
            else if (top10HoldersPct > 30.0) score -= 10

            if (!isMintRenounced) score -= 20
            if (!isFreezeDisabled) score -= 25
            if (!isLpBurned) score -= 10
            if (honeypotRisk) score -= 40
            if (bundledSnipersCount > 5) score -= 15

            return score.coerceIn(5, 99)
        }

    val riskLevel: RiskLevel
        get() = when {
            !isVerified -> RiskLevel.UNKNOWN
            safetyScore >= 80 -> RiskLevel.SAFE
            safetyScore >= 55 -> RiskLevel.MODERATE
            else -> RiskLevel.HIGH_RISK
        }
}

enum class RiskLevel {
    SAFE,
    MODERATE,
    HIGH_RISK,
    UNKNOWN
}

data class PumpToken(
    val id: String, // Mint contract address
    val name: String,
    val symbol: String,
    val description: String,
    val avatarEmoji: String, // Instant meme visual icon
    val avatarBgColor: Long,
    val createdAt: Long,
    val priceUsd: Double,
    val priceSol: Double,
    val change1m: Double,
    val change5m: Double,
    val change1h: Double,
    val marketCapUsd: Double,
    val volume24hUsd: Double,
    val bondingCurveProgress: Float, // 0.0 to 1.0
    val holderCount: Int,
    val telegramUrl: String,
    val twitterUrl: String,
    val websiteUrl: String,
    val dexPaidStatus: DexPaidStatus,
    val dexPaidTime: Long?,
    val rugAnalysis: RugAnalysis,
    val candles: List<PriceCandle>,
    val stage: TokenLaunchStage,
    val isFavorite: Boolean = false,
    val bondingCurveKnown: Boolean = true,
    val imageUrl: String = "",
    val pairUrl: String = ""
) {
    val formattedMarketCap: String
        get() = when {
            marketCapUsd >= 1_000_000 -> "$${String.format("%.2fM", marketCapUsd / 1_000_000)}"
            marketCapUsd >= 1_000 -> "$${String.format("%.1fK", marketCapUsd / 1_000)}"
            else -> "$${String.format("%.0f", marketCapUsd)}"
        }

    val formattedVolume: String
        get() = when {
            volume24hUsd >= 1_000_000 -> "$${String.format("%.2fM", volume24hUsd / 1_000_000)}"
            volume24hUsd >= 1_000 -> "$${String.format("%.1fK", volume24hUsd / 1_000)}"
            else -> "$${String.format("%.0f", volume24hUsd)}"
        }

    val shortAddress: String
        get() = if (id.length > 8) "${id.take(4)}...${id.takeLast(4)}" else id
}
