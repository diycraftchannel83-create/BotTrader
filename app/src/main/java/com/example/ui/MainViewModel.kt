package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.PumpFunRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class TokenFilter {
    ALL,
    DEX_PAID,
    BONDING,
    GRADUATED,
    DEX_LISTED,
    SAFE_ONLY,
    TOP_GAINERS
}

enum class TokenSort {
    NEWEST,
    BONDING_PROGRESS,
    PRICE_GAIN,
    MARKET_CAP,
    SAFETY_SCORE
}

data class PortfolioSummary(
    val totalBalanceSol: Double,
    val totalBalanceUsd: Double,
    val totalInvestedSol: Double,
    val unrealizedPnlSol: Double,
    val unrealizedPnlPct: Double,
    val realizedPnlSol: Double,
    val totalTrades: Int,
    val winCount: Int,
    val winRatePct: Double
)

class MainViewModel(
    val repository: PumpFunRepository = PumpFunRepository()
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(TokenFilter.ALL)
    val activeFilter: StateFlow<TokenFilter> = _activeFilter.asStateFlow()

    private val _activeSort = MutableStateFlow(TokenSort.NEWEST)
    val activeSort: StateFlow<TokenSort> = _activeSort.asStateFlow()

    private val _selectedTokenId = MutableStateFlow<String?>(null)
    val selectedTokenId: StateFlow<String?> = _selectedTokenId.asStateFlow()

    val tokens: StateFlow<List<PumpToken>> = repository.tokens
    val walletSolBalance: StateFlow<Double> = repository.walletSolBalance
    val currentWallet: StateFlow<SolanaWallet> = repository.currentWallet
    val holdings: StateFlow<List<PortfolioHolding>> = repository.holdings
    val limitOrders: StateFlow<List<LimitOrder>> = repository.limitOrders
    val transactions: StateFlow<List<TradeTransaction>> = repository.transactions
    val alerts: StateFlow<List<PriceAlert>> = repository.alerts
    val botConfig: StateFlow<BotStrategyConfig> = repository.botConfig
    val recentNotification: StateFlow<PriceAlert?> = repository.recentNotification
    val solPriceUsd: StateFlow<Double> = repository.solPriceUsd
    val marketFeedStatus: StateFlow<com.example.data.repository.MarketFeedStatus> = repository.marketFeedStatus
    val lastMarketUpdate: StateFlow<Long?> = repository.lastMarketUpdate
    val marketError: StateFlow<String?> = repository.marketError

    val filteredTokens: StateFlow<List<PumpToken>> = combine(
        tokens,
        _searchQuery,
        _activeFilter,
        _activeSort
    ) { tokenList, query, filter, sort ->
        var list = tokenList.filter {
            if (query.isBlank()) true
            else it.name.contains(query, ignoreCase = true) ||
                 it.symbol.contains(query, ignoreCase = true) ||
                 it.id.contains(query, ignoreCase = true)
        }

        list = when (filter) {
            TokenFilter.ALL -> list
            TokenFilter.DEX_PAID -> list.filter { it.dexPaidStatus == DexPaidStatus.CONFIRMED }
            TokenFilter.BONDING -> list.filter { it.stage == TokenLaunchStage.BONDING }
            TokenFilter.GRADUATED -> list.filter { it.stage == TokenLaunchStage.GRADUATED }
            TokenFilter.DEX_LISTED -> list.filter { it.stage == TokenLaunchStage.DEX_LISTED }
            TokenFilter.SAFE_ONLY -> list.filter { it.rugAnalysis.isVerified && it.rugAnalysis.safetyScore >= 80 }
            TokenFilter.TOP_GAINERS -> list.filter { it.change5m > 0 }.sortedByDescending { it.change5m }
        }

        when (sort) {
            TokenSort.NEWEST -> list.sortedByDescending { it.createdAt }
            TokenSort.BONDING_PROGRESS -> list.sortedByDescending { it.bondingCurveProgress }
            TokenSort.PRICE_GAIN -> list.sortedByDescending { it.change5m }
            TokenSort.MARKET_CAP -> list.sortedByDescending { it.marketCapUsd }
            TokenSort.SAFETY_SCORE -> list.sortedByDescending { it.rugAnalysis.safetyScore }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedToken: StateFlow<PumpToken?> = combine(tokens, _selectedTokenId) { tokenList, id ->
        if (id == null) null
        else tokenList.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val portfolioSummary: StateFlow<PortfolioSummary> = combine(
        walletSolBalance,
        holdings,
        transactions,
        tokens,
        solPriceUsd
    ) { walletSol, holdingList, txList, tokenList, currentSolPriceUsd ->
        var holdingsValueSol = 0.0
        var totalInvestedSol = 0.0

        holdingList.forEach { holding ->
            val curToken = tokenList.find { it.id == holding.tokenId }
            val curPrice = curToken?.priceSol ?: holding.entryPriceSol
            holdingsValueSol += holding.amountTokens * curPrice
            totalInvestedSol += holding.totalInvestedSol
        }

        val totalPortfolioSol = walletSol + holdingsValueSol
        val totalPortfolioUsd = totalPortfolioSol * currentSolPriceUsd
        val unrealizedPnlSol = holdingsValueSol - totalInvestedSol
        val unrealizedPnlPct = if (totalInvestedSol > 0) (unrealizedPnlSol / totalInvestedSol) * 100.0 else 0.0

        val sellTxs = txList.filter { it.type == "SELL" && it.realizedPnlSol != null }
        val realizedPnlSol = sellTxs.sumOf { it.realizedPnlSol ?: 0.0 }
        val totalTrades = txList.size
        val winCount = sellTxs.count { (it.realizedPnlSol ?: 0.0) > 0 }
        val winRatePct = if (sellTxs.isNotEmpty()) (winCount.toDouble() / sellTxs.size) * 100.0 else 0.0

        PortfolioSummary(
            totalBalanceSol = totalPortfolioSol,
            totalBalanceUsd = totalPortfolioUsd,
            totalInvestedSol = totalInvestedSol,
            unrealizedPnlSol = unrealizedPnlSol,
            unrealizedPnlPct = unrealizedPnlPct,
            realizedPnlSol = realizedPnlSol,
            totalTrades = totalTrades,
            winCount = winCount,
            winRatePct = winRatePct
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PortfolioSummary(5.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0.0)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: TokenFilter) {
        _activeFilter.value = filter
    }

    fun setSort(sort: TokenSort) {
        _activeSort.value = sort
    }

    fun selectToken(tokenId: String?) {
        _selectedTokenId.value = tokenId
    }

    fun buyToken(tokenId: String, amountSol: Double): Boolean {
        return repository.buyToken(tokenId, amountSol, isAutoBot = false)
    }

    fun sellToken(tokenId: String, amountTokens: Double): Boolean {
        return repository.sellToken(tokenId, amountTokens, isAutoBot = false)
    }

    fun addLimitOrder(
        tokenId: String,
        orderType: OrderType,
        targetPriceSol: Double,
        amountSol: Double,
        slippagePct: Double = 5.0
    ) {
        repository.addLimitOrder(tokenId, orderType, targetPriceSol, amountSol, slippagePct)
    }

    fun cancelLimitOrder(orderId: String) {
        repository.cancelLimitOrder(orderId)
    }

    fun toggleBot(enabled: Boolean) {
        repository.updateBotConfig(repository.botConfig.value.copy(isMasterEnabled = enabled))
    }

    fun updateBotConfig(config: BotStrategyConfig) {
        repository.updateBotConfig(config)
    }

    fun toggleFavorite(tokenId: String) {
        repository.toggleFavorite(tokenId)
    }

    fun dismissNotification() {
        repository.clearRecentNotification()
    }

    fun refreshMarketData() {
        repository.refreshMarketData()
    }

    fun triggerNewTokenLaunch() {
        refreshMarketData()
    }

    fun resetWallet() {
        repository.resetWalletBalance(5.0)
    }

    fun importPrivateKey(rawKey: String, customLabel: String): Result<SolanaWallet> {
        return repository.importPrivateKey(rawKey, customLabel)
    }

    fun disconnectWallet() {
        repository.disconnectWallet()
    }

    fun withdrawSol(recipientAddress: String, amountSol: Double): Result<TradeTransaction> {
        return repository.withdrawSol(recipientAddress, amountSol)
    }
}
