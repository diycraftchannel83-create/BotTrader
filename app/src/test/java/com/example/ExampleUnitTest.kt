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
    fun testSolanaPrivateKeyValidationAndDerivation() {
        // Sample Base58 key
        val sampleKey = "5MaiiCavjCmn9Hs1o3eznqDE2BEWQ52jA6hac2MDB98DzpHzPQGybJBcZcaqPMNzoyG2g"
        assertTrue(com.example.util.SolanaKeyUtils.isValidPrivateKey(sampleKey))

        val derivedAddress = com.example.util.SolanaKeyUtils.deriveSolanaAddress(sampleKey)
        assertEquals(44, derivedAddress.length)
        assertTrue(com.example.util.SolanaKeyUtils.isValidSolanaAddress(derivedAddress))

        // Sample Byte Array key
        val byteArrKey = "[142, 23, 51, 88, 99, 12, 45, 67, 89, 120, 200, 250, 11, 22, 33, 44, 55, 66, 77, 88, 99, 111, 222, 133, 144, 155, 166, 177, 188, 199, 210, 230]"
        assertTrue(com.example.util.SolanaKeyUtils.isValidPrivateKey(byteArrKey))
    }

    @Test
    fun testWithdrawalExecutionInRepository() {
        val repo = com.example.data.repository.PumpFunRepository()
        val initialBalance = repo.walletSolBalance.value

        val destination = "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM"
        val withdrawAmt = 1.5

        val result = repo.withdrawSol(destination, withdrawAmt)
        assertTrue(result.isSuccess)
        val tx = result.getOrNull()
        assertNotNull(tx)
        assertEquals("WITHDRAW", tx?.type)
        assertEquals(withdrawAmt, tx?.amountSol ?: 0.0, 0.0001)
        assertEquals(destination, tx?.recipientAddress)

        // Balance should decrease by 1.5 + fee (0.000005)
        val expectedBalance = initialBalance - (withdrawAmt + 0.000005)
        assertEquals(expectedBalance, repo.walletSolBalance.value, 0.0001)

        // Attempting to withdraw more than available balance should fail
        val excessiveResult = repo.withdrawSol(destination, 999.0)
        assertTrue(excessiveResult.isFailure)
    }
}
