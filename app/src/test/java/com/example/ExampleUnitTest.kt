package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
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
}
