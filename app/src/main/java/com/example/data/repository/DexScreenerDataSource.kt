package com.example.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class DexScreenerMarketToken(
    val mintAddress: String,
    val name: String,
    val symbol: String,
    val description: String,
    val iconUrl: String,
    val priceUsd: Double,
    val change5mPct: Double,
    val change1hPct: Double,
    val marketCapUsd: Double,
    val volume24hUsd: Double,
    val liquidityUsd: Double,
    val pairCreatedAt: Long,
    val telegramUrl: String,
    val twitterUrl: String,
    val websiteUrl: String,
    val pairUrl: String
)

internal data class DexScreenerMarketSnapshot(
    val solPriceUsd: Double,
    val tokens: List<DexScreenerMarketToken>
)

/**
 * Read-only public market data client.
 *
 * This client never builds, signs, or submits transactions. DexScreener does not provide
 * holder concentration, mint/freeze authority, honeypot simulation, or Pump.fun bonding
 * curve progress through these endpoints; those values must remain explicitly unverified.
 */
class DexScreenerDataSource(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .callTimeout(22, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetchLatestSolanaMarket(): DexScreenerMarketSnapshot = withContext(Dispatchers.IO) {
        val profiles = getJsonArray("https://api.dexscreener.com/token-profiles/latest/v1")
        val profileByAddress = linkedMapOf<String, JSONObject>()
        for (index in 0 until profiles.length()) {
            val profile = profiles.optJSONObject(index) ?: continue
            if (!profile.optString("chainId").equals("solana", ignoreCase = true)) continue
            val address = profile.optString("tokenAddress").trim()
            if (address.isNotEmpty()) profileByAddress.putIfAbsent(address, profile)
        }

        val addresses = profileByAddress.keys.take(MAX_TOKEN_ADDRESSES)
        val pairArray = if (addresses.isEmpty()) JSONArray() else {
            getJsonArray("https://api.dexscreener.com/tokens/v1/solana/${addresses.joinToString(",")}")
        }

        val bestPairByMint = linkedMapOf<String, DexScreenerMarketToken>()
        for (index in 0 until pairArray.length()) {
            val pair = pairArray.optJSONObject(index) ?: continue
            if (!pair.optString("chainId").equals("solana", ignoreCase = true)) continue
            val base = pair.optJSONObject("baseToken") ?: continue
            val mint = base.optString("address").trim()
            if (mint !in profileByAddress) continue

            val profile = profileByAddress[mint] ?: JSONObject()
            val token = parseToken(pair, profile) ?: continue
            val previous = bestPairByMint[mint]
            if (previous == null || token.liquidityUsd > previous.liquidityUsd) {
                bestPairByMint[mint] = token
            }
        }

        val solPairs = getJsonArray(
            "https://api.dexscreener.com/tokens/v1/solana/$WRAPPED_SOL_MINT"
        )
        val solPriceUsd = (0 until solPairs.length())
            .mapNotNull { solPairs.optJSONObject(it) }
            .filter { pair ->
                pair.optString("chainId").equals("solana", ignoreCase = true) &&
                    pair.optJSONObject("baseToken")?.optString("address") == WRAPPED_SOL_MINT &&
                    pair.optJSONObject("quoteToken")?.optString("symbol")
                        ?.uppercase() in setOf("USDC", "USDT")
            }
            .maxByOrNull { it.optJSONObject("liquidity")?.optDouble("usd", 0.0) ?: 0.0 }
            ?.optString("priceUsd")
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?: throw IOException("DexScreener did not return a valid SOL/USD market price.")

        DexScreenerMarketSnapshot(
            solPriceUsd = solPriceUsd,
            tokens = bestPairByMint.values
                .filter { it.priceUsd > 0.0 && it.liquidityUsd > 0.0 }
                .sortedByDescending { it.liquidityUsd }
        )
    }

    private fun parseToken(pair: JSONObject, profile: JSONObject): DexScreenerMarketToken? {
        val base = pair.optJSONObject("baseToken") ?: return null
        val info = pair.optJSONObject("info") ?: JSONObject()
        val priceUsd = pair.optString("priceUsd").toDoubleOrNull() ?: return null
        if (!priceUsd.isFinite() || priceUsd <= 0.0) return null

        val priceChange = pair.optJSONObject("priceChange") ?: JSONObject()
        val volume = pair.optJSONObject("volume") ?: JSONObject()
        val liquidity = pair.optJSONObject("liquidity") ?: JSONObject()
        val websites = info.optJSONArray("websites") ?: JSONArray()
        val socials = info.optJSONArray("socials") ?: JSONArray()
        val profileLinks = profile.optJSONArray("links") ?: JSONArray()

        fun findSocial(platform: String): String {
            for (i in 0 until socials.length()) {
                val item = socials.optJSONObject(i) ?: continue
                if (item.optString("type").equals(platform, true) ||
                    item.optString("platform").equals(platform, true)
                ) return item.optString("url")
            }
            for (i in 0 until profileLinks.length()) {
                val item = profileLinks.optJSONObject(i) ?: continue
                if (item.optString("type").equals(platform, true) ||
                    item.optString("platform").equals(platform, true)
                ) return item.optString("url")
            }
            return ""
        }

        val website = (0 until websites.length())
            .mapNotNull { websites.optJSONObject(it)?.optString("url") }
            .firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
            ?: (0 until profileLinks.length())
                .mapNotNull { profileLinks.optJSONObject(it)?.optString("url") }
                .firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
                .orEmpty()

        return DexScreenerMarketToken(
            mintAddress = base.optString("address"),
            name = base.optString("name").ifBlank { base.optString("symbol") },
            symbol = base.optString("symbol"),
            description = profile.optString("description"),
            iconUrl = info.optString("imageUrl").ifBlank { profile.optString("icon") },
            priceUsd = priceUsd,
            change5mPct = priceChange.optDouble("m5", 0.0).takeIf { it.isFinite() } ?: 0.0,
            change1hPct = priceChange.optDouble("h1", 0.0).takeIf { it.isFinite() } ?: 0.0,
            marketCapUsd = (pair.optDouble("marketCap", 0.0).takeIf { it > 0.0 }
                ?: pair.optDouble("fdv", 0.0)).takeIf { it.isFinite() } ?: 0.0,
            volume24hUsd = volume.optDouble("h24", 0.0).takeIf { it.isFinite() } ?: 0.0,
            liquidityUsd = liquidity.optDouble("usd", 0.0).takeIf { it.isFinite() } ?: 0.0,
            pairCreatedAt = pair.optLong("pairCreatedAt", 0L),
            telegramUrl = findSocial("telegram"),
            twitterUrl = findSocial("twitter").ifBlank { findSocial("x") },
            websiteUrl = website,
            pairUrl = pair.optString("url")
        ).takeIf { it.mintAddress.isNotBlank() && it.symbol.isNotBlank() }
    }

    private fun getJsonArray(url: String): JSONArray {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "BotTrader-Android/1.0")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("DexScreener HTTP ${response.code} for market data request.")
            }
            val body = response.body?.string()
                ?: throw IOException("DexScreener returned an empty response.")
            return JSONArray(body)
        }
    }

    private companion object {
        const val MAX_TOKEN_ADDRESSES = 30
        const val WRAPPED_SOL_MINT = "So11111111111111111111111111111111111111112"
    }
}
