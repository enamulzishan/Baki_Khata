package com.example.utils

import com.example.Customer

object MessageUtils {
    fun buildReminderMessage(customer: Customer): String {
        return "প্রিয় ${customer.name}, আপনার কাছে আমাদের ${CurrencyFormatter.format(customer.totalDue)} বকেয়া আছে। দয়া করে সময়মতো পরিশোধ করুন। ধন্যবাদ।"
    }
}
