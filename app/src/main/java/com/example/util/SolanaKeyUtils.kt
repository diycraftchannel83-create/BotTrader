package com.example.util

/**
 * Format-only helpers. These functions do NOT prove key ownership or derive Solana public keys.
 * Real wallet support must use a maintained, audited Solana Ed25519 implementation.
 */
object SolanaKeyUtils {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun isValidSolanaAddress(address: String): Boolean {
        val value = address.trim()
        if (value.length !in 32..44 || value.any { it !in ALPHABET }) return false
        return try {
            decodeBase58(value).size == 32
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /**
     * Checks only the textual shape of common key exports. This is NOT cryptographic validation.
     * Never use this result to authorize signing or wallet ownership.
     */
    fun isValidPrivateKey(key: String): Boolean {
        val value = key.trim()
        if (value.startsWith("[") && value.endsWith("]")) {
            val parts = value.removeSurrounding("[", "]").split(",")
            if (parts.size !in 32..64) return false
            return parts.all { part -> part.trim().toIntOrNull()?.let { it in 0..255 } == true }
        }
        if (value.length in 40..90 && value.all { it in ALPHABET }) {
            return try { decodeBase58(value).size in 32..64 } catch (_: IllegalArgumentException) { false }
        }
        if (value.length in 64..128 && value.length % 2 == 0 &&
            value.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) return true
        return false
    }

    @Deprecated("Not implemented: use an audited Solana Ed25519 key derivation library.")
    fun deriveSolanaAddress(privateKey: String): String {
        throw UnsupportedOperationException(
            "Solana public-key derivation is unavailable. Do not use a hash-derived address."
        )
    }

    fun maskPrivateKey(key: String): String {
        val value = key.trim()
        return if (value.length > 8) "••••••••••••••••${value.takeLast(4)}" else "••••••••"
    }

    @Deprecated("A transaction signature must come from real Ed25519 signing and chain submission.")
    fun generateTxSignature(): String {
        throw UnsupportedOperationException(
            "Fake transaction signatures are disabled. A signature is available only after signing and submitting a real transaction."
        )
    }

    private fun decodeBase58(value: String): ByteArray {
        if (value.isEmpty()) return byteArrayOf()
        var number = java.math.BigInteger.ZERO
        val base = java.math.BigInteger.valueOf(58)
        value.forEach { char ->
            val digit = ALPHABET.indexOf(char)
            require(digit >= 0) { "Invalid Base58 character" }
            number = number.multiply(base).add(java.math.BigInteger.valueOf(digit.toLong()))
        }
        val decoded = if (number == java.math.BigInteger.ZERO) byteArrayOf() else number.toByteArray().let {
            if (it.firstOrNull() == 0.toByte()) it.copyOfRange(1, it.size) else it
        }
        val leadingZeros = value.takeWhile { it == '1' }.length
        return ByteArray(leadingZeros) + decoded
    }
}
