package com.example.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    private val format = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    fun format(amount: Long): String {
        return "৳ ${format.format(amount)}"
    }
    
    fun format(amount: Double): String {
        return "৳ ${format.format(amount)}"
    }
}
