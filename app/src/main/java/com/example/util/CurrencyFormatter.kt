package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val rupiahFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
            currencySymbol = "Rp "
            groupingSeparator = '.'
            monetaryDecimalSeparator = ','
        }
        DecimalFormat("Rp #,##0", symbols)
    }

    private val numberOnlyFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,##0", symbols)
    }

    fun formatRupiah(amount: Double): String {
        return rupiahFormat.format(amount)
    }

    fun formatNumberOnly(amount: Double): String {
        return numberOnlyFormat.format(amount)
    }

    fun formatThousands(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        val truncated = if (digits.length > 15) digits.take(15) else digits
        val number = truncated.toLongOrNull() ?: return digits
        return numberOnlyFormat.format(number)
    }

    fun formatCompactRupiah(amount: Double): String {
        val isNegative = amount < 0
        val abs = kotlin.math.abs(amount)
        val prefix = if (isNegative) "-Rp " else "Rp "
        return when {
            abs >= 1_000_000_000 -> String.format(Locale.forLanguageTag("id-ID"), "%s%.1f M", prefix, abs / 1_000_000_000)
            abs >= 1_000_000 -> String.format(Locale.forLanguageTag("id-ID"), "%s%.1f Jt", prefix, abs / 1_000_000)
            abs >= 1_000 -> String.format(Locale.forLanguageTag("id-ID"), "%s%.0f Rb", prefix, abs / 1_000)
            else -> formatRupiah(amount)
        }
    }

    fun formatPercent(percent: Double): String {
        val prefix = if (percent > 0) "+" else ""
        return String.format(Locale.forLanguageTag("id-ID"), "%s%.2f%%", prefix, percent)
    }
}
