package com.example

import com.example.data.model.*
import com.example.util.TokenPriceFormatter
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun candidateScoreUsesEditableRelativeWeightsAndStaysWithinRange() {
        val token = PumpToken(
            id = "MintScoringTest", name = "Scoring Test", symbol = "SCORE",
            description = "", avatarEmoji = "🪙", avatarBgColor = 0L,
            createdAt = System.currentTimeMillis() - 60_000L,
            priceUsd = 0.01, priceSol = 0.0001, change1m = 0.0,
            change5m = 10.0, change1h = 20.0, marketCapUsd = 100_000.0,
            volume24hUsd = 100_000.0, bondingCurveProgress = 0f, holderCount = 0,
            telegramUrl = "", twitterUrl = "", websiteUrl = "",
            dexPaidStatus = DexPaidStatus.PENDING, dexPaidTime = null,
            rugAnalysis = RugAnalysis(0.0, 0.0, false, false, false, false, 0, emptyList()),
            candles = emptyList(), stage = TokenLaunchStage.DEX_LISTED,
            bondingCurveKnown = false, liquidityUsd = 100_000.0
        )
        val liquidityOnly = BotStrategyConfig(
            scoreLiquidityWeight = 100, scoreVolumeWeight = 0, scoreMomentumWeight = 0,
            scoreAgeWeight = 0, scoreMarketCapWeight = 0, scoreTrendWeight = 0
        )
        val trendOnly = liquidityOnly.copy(scoreLiquidityWeight = 0, scoreTrendWeight = 100)

        val liquidityScore = com.example.data.repository.SmartTradingStrategy.assess(token, liquidityOnly).score
        val trendScore = com.example.data.repository.SmartTradingStrategy.assess(token, trendOnly).score

        assertTrue(liquidityScore in 0..100)
        assertTrue(trendScore in 0..100)
        assertNotEquals(liquidityScore, trendScore)
    }

    @Test
    fun antiMevGuardrailsDefaultToEnabledWithConservativeSlippageAndPrivateRoutingRequired() {
        val config = BotStrategyConfig()
        assertTrue(config.antiMevProtectionEnabled)
        assertEquals(1.0, config.maxSlippagePct, 0.0)
        assertEquals(5_000L, config.maxQuoteAgeMs)
        assertTrue(config.requirePrivateRoutingForLive)
    }

    @Test
    fun tokenPriceFormatterKeepsTenDecimalPlacesForTinyPrices() {
        assertEquals("0.0000000123", TokenPriceFormatter.format(0.0000000123))
        assertEquals("1.2345678901", TokenPriceFormatter.format(1.2345678901))
    }

    @Test
    fun testRugPullSafetyScore_SafeToken() {
        val safeAnalysis = RugAnalysis(
            devHoldingPct = 1.5,
            top10HoldersPct = 20.0,
            isMintRenounced = true,
            isFreezeDisabled = true,
            isLpBurned = true,
            honeypotRisk = false,
            bundledSnipersCount = 0,
            warnings = emptyList()
        )
        assertTrue("Safe token should have score >= 80", safeAnalysis.safetyScore >= 80)
        assertEquals(RiskLevel.SAFE, safeAnalysis.riskLevel)
    }

    @Test
    fun unverifiedMarketDataMustNotReceiveASafetyScore() {
        val unverified = RugAnalysis(
            devHoldingPct = 0.0,
            top10HoldersPct = 0.0,
            isMintRenounced = false,
            isFreezeDisabled = false,
            isLpBurned = false,
            honeypotRisk = false,
            bundledSnipersCount = 0,
            warnings = listOf("Security data not verified"),
            isVerified = false
        )

        assertEquals(0, unverified.safetyScore)
        assertEquals(RiskLevel.UNKNOWN, unverified.riskLevel)
    }
    @Test
    fun testRugPullSafetyScore_DangerousToken() {
        val dangerousAnalysis = RugAnalysis(
            devHoldingPct = 35.0,
            top10HoldersPct = 80.0,
            isMintRenounced = false,
            isFreezeDisabled = false,
            isLpBurned = false,
            honeypotRisk = true,
            bundledSnipersCount = 15,
            warnings = listOf("Honeypot risk", "Dev holds 35%")
        )
        assertTrue("Dangerous token should have low score", dangerousAnalysis.safetyScore < 50)
        assertEquals(RiskLevel.HIGH_RISK, dangerousAnalysis.riskLevel)
    }

    @Test
    fun autoBuyDefaultsToPointZeroOneSolAndRequiresExplicitEnablement() {
        val config = BotStrategyConfig()
        assertEquals(0.01, config.buyAmountSol, 0.0)
        assertFalse(config.autoBuyEnabled)
        assertFalse(config.allowUnverifiedSecurityData)
    }

    @Test
    fun smartExitTriggersStopLossTakeProfitAndTrailingStop() {
        val stop = com.example.data.repository.SmartTradingStrategy.evaluateExit(-21.0, 0.0, 100.0, 20.0, 15.0)
        assertTrue(stop.shouldSell)
        assertEquals("STOP_LOSS", stop.reason)

        val target = com.example.data.repository.SmartTradingStrategy.evaluateExit(100.0, 100.0, 100.0, 20.0, 15.0)
        assertTrue(target.shouldSell)
        assertEquals("TAKE_PROFIT", target.reason)

        val trailing = com.example.data.repository.SmartTradingStrategy.evaluateExit(20.0, 40.0, 100.0, 20.0, 15.0)
        assertTrue(trailing.shouldSell)
        assertEquals("TRAILING_STOP", trailing.reason)
    }

    @Test
    fun smartCandidateScannerBlocksUnverifiedTokenByDefault() {
        val token = PumpToken(
            id = "MintAddressExample",
            name = "Example",
            symbol = "EX",
            description = "Test token",
            avatarEmoji = "🪙",
            avatarBgColor = 0L,
            createdAt = System.currentTimeMillis(),
            priceUsd = 0.01,
            priceSol = 0.0001,
            change1m = 0.0,
            change5m = 10.0,
            change1h = 20.0,
            marketCapUsd = 100_000.0,
            volume24hUsd = 100_000.0,
            bondingCurveProgress = 0f,
            holderCount = 0,
            telegramUrl = "",
            twitterUrl = "",
            websiteUrl = "",
            dexPaidStatus = DexPaidStatus.PENDING,
            dexPaidTime = null,
            rugAnalysis = RugAnalysis(0.0, 0.0, false, false, false, false, 0, emptyList(), isVerified = false),
            candles = emptyList(),
            stage = TokenLaunchStage.DEX_LISTED,
            bondingCurveKnown = false,
            liquidityUsd = 100_000.0
        )
        val result = com.example.data.repository.SmartTradingStrategy.assess(token, BotStrategyConfig())
        assertFalse(result.eligible)
        assertTrue(result.blockers.any { it.contains("unverified") })
    }
    @Test
    fun testPortfolioHoldingPnlCalculation() {
        val holding = PortfolioHolding(
            tokenId = "test_1",
            tokenSymbol = "TEST",
            tokenName = "Test Coin",
            avatarEmoji = "🚀",
            amountTokens = 1000.0,
            entryPriceSol = 0.001,
            totalInvestedSol = 1.0 // 1000 * 0.001
        )
        // If price doubles to 0.002
        val currentPrice = 0.002
        val curVal = holding.currentValSol(currentPrice)
        val pnlSol = holding.pnlSol(currentPrice)
        val pnlPct = holding.pnlPct(currentPrice)

        assertEquals(2.0, curVal, 0.0001)
        assertEquals(1.0, pnlSol, 0.0001)
        assertEquals(100.0, pnlPct, 0.0001)
    }

    @Test
    fun testLimitOrderTriggerConditions() {
        val buyLimitOrder = LimitOrder(
            id = "order_1",
            tokenId = "token_1",
            tokenSymbol = "MOON",
            tokenName = "Moon Coin",
            orderType = OrderType.BUY_LIMIT,
            targetPriceSol = 0.0005,
            triggerCondition = "<=",
            amountSol = 0.5
        )
        // Token price drops to 0.0004 -> should trigger
        val currentPrice = 0.0004
        val shouldTrigger = currentPrice <= buyLimitOrder.targetPriceSol
        assertTrue("Buy limit order should trigger when price drops to or below target", shouldTrigger)
    }

    @Test
    fun testSolanaAddressValidation() {
        val validSolanaAddr = "7xKXtg2CW87d97TXJSDpbD5jBkheTqA83TZRuJosgAsU"
        assertTrue(com.example.util.SolanaKeyUtils.isValidSolanaAddress(validSolanaAddr))

        val invalidAddr = "0x12345" // Ethereum address
        assertFalse(com.example.util.SolanaKeyUtils.isValidSolanaAddress(invalidAddr))

        val invalidChars = "0OIl_invalid_solana_address"
        assertFalse(com.example.util.SolanaKeyUtils.isValidSolanaAddress(invalidChars))
    }

    @Test
    fun fakeWalletDerivationIsExplicitlyDisabled() {
        val sampleKey = "5MaiiCavjCmn9Hs1o3eznqDE2BEWQ52jA6hac2MDB98DzpHzPQGybJBcZcaqPMNzoyG2g"
        assertTrue(com.example.util.SolanaKeyUtils.isValidPrivateKey(sampleKey))
        try {
            com.example.util.SolanaKeyUtils.deriveSolanaAddress(sampleKey)
            fail("Hash-based public-key derivation must not be used")
        } catch (_: UnsupportedOperationException) {
            // Expected until audited Ed25519 derivation is integrated.
        }
    }

    @Test
    fun fakeTransactionSignaturesAreDisabled() {
        try {
            com.example.util.SolanaKeyUtils.generateTxSignature()
            fail("A random string must never be represented as a Solana signature")
        } catch (_: UnsupportedOperationException) {
            // Expected: no signing implementation is connected.
        }
    }

    @Test
    fun paperWalletCannotImportKeysOrWithdraw() {
        val repo = com.example.data.repository.PumpFunRepository(autoStartMarketFeed = false)
        assertTrue(repo.importPrivateKey("not-a-real-key").isFailure)
        assertTrue(repo.withdrawSol("9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM", 1.0).isFailure)
        assertEquals(5.0, repo.walletSolBalance.value, 0.0001)
    }

}
