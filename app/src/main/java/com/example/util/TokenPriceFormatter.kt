package com.example.util

import java.util.Locale

/**
 * Formats token prices with fixed precision so very low-cap token prices
 * remain readable without being rounded to zero.
 *
 * Locale.US is intentional: financial values always use a dot decimal
 * separator regardless of the device language.
 */
object TokenPriceFormatter {
    fun format(value: Double): String = String.format(Locale.US, "%.10f", value)
}
