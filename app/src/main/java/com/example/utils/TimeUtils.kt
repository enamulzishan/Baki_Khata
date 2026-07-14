package com.example.utils
import java.util.concurrent.TimeUnit
object TimeUtils {
    fun getRelativeTime(timestamp: Long): String {
        if (timestamp == 0L) return "কখনও না"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            days == 0L -> "আজ"
            days == 1L -> "গতকাল"
            days in 2..6 -> "${days} দিন আগে"
            days in 7..13 -> "১ সপ্তাহ আগে"
            days in 14..29 -> "${days / 7} সপ্তাহ আগে"
            days in 30..59 -> "১ মাস আগে"
            days in 60..364 -> "${days / 30} মাস আগে"
            else -> "${days / 365} বছর আগে"
        }
    }
}
