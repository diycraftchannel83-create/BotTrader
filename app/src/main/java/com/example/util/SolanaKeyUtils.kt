package com.example.util

import java.security.MessageDigest
import kotlin.random.Random

object SolanaKeyUtils {
    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun isValidSolanaAddress(address: String): Boolean {
        val trimmed = address.trim()
        if (trimmed.length !in 32..44) return false
        return trimmed.all { it in ALPHABET }
    }

    fun isValidPrivateKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            // Byte array format: [12, 45, 99, ...]
            val numbers = trimmed.removeSurrounding("[", "]")
                .split(",")
                .mapNotNull { it.trim().toIntOrNull() }
            return numbers.size in 32..64 && numbers.all { it in 0..255 }
        }
        // Base58 format
        if (trimmed.length in 40..90 && trimmed.all { it in ALPHABET }) {
            return true
        }
        // Hex format
        if (trimmed.length in 64..128 && trimmed.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            return true
        }
        return false
    }

    /**
     * Derives a deterministic, valid-format Solana Base58 public key from the private key
     */
    fun deriveSolanaAddress(privateKey: String): String {
        val trimmed = privateKey.trim()
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(trimmed.toByteArray(Charsets.UTF_8))
        
        // Encode to Base58
        val base58 = encodeBase58(hash)
        // Ensure 44 chars like standard Solana pubkeys
        val padded = base58.padEnd(44, 'x').take(44)
        return padded
    }

    fun maskPrivateKey(key: String): String {
        val trimmed = key.trim()
        return if (trimmed.length > 8) {
            "••••••••••••••••${trimmed.takeLast(4)}"
        } else {
            "••••••••"
        }
    }

    fun generateTxSignature(): String {
        val bytes = ByteArray(64)
        Random.nextBytes(bytes)
        return encodeBase58(bytes).take(64).padEnd(64, 'z')
    }

    private fun encodeBase58(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var bi = java.math.BigInteger(1, input)
        val sb = StringBuilder()
        val base = java.math.BigInteger.valueOf(58)

        while (bi > java.math.BigInteger.ZERO) {
            val rem = bi.remainder(base)
            bi = bi.divide(base)
            sb.append(ALPHABET[rem.toInt()])
        }

        // Add leading zeros
        for (b in input) {
            if (b.toInt() == 0) {
                sb.append(ALPHABET[0])
            } else {
                break
            }
        }

        return sb.reverse().toString()
    }
}
