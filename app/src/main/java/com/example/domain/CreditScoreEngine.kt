package com.example.domain

import com.example.Customer
import com.example.Transaction
import java.util.Date

enum class RiskLevel(val label: String, val stars: String, val color: Long) {
    EXCELLENT("Excellent", "★★★★★", 0xFF4CAF50), // Green
    VERY_GOOD("Very Good", "★★★★☆", 0xFF8BC34A), // Light Green
    GOOD("Good", "★★★☆☆", 0xFFFFC107), // Yellow
    RISKY("Risky", "★★☆☆☆", 0xFFFF9800), // Orange
    HIGH_RISK("High Risk", "★☆☆☆☆", 0xFFF44336) // Red
}

data class CreditProfile(
    val score: Int,
    val riskLevel: RiskLevel,
    val averagePaymentTimeDays: Long,
    val longestOverdueDays: Long,
    val totalPurchases: Long,
    val totalCollections: Long,
    val outstandingDue: Long,
    val lastPaymentDate: Date?,
    val insights: List<String>
)

object CreditScoreEngine {

    fun calculateProfile(customer: Customer, transactions: List<Transaction>): CreditProfile {
        val sortedTx = transactions.sortedBy { it.timestamp.toDate() }
        
        var score = 100
        var maxOverdueDays = 0L
        var totalDelayDays = 0L
        var paymentCount = 0
        var lastPaymentDate: Date? = null
        
        var totalPurchases = 0L
        var totalCollections = 0L
        
        var currentDue = 0L
        var lastDueDate: Date? = null
        
        val insights = mutableListOf<String>()

        for (tx in sortedTx) {
            if (tx.type == "DUE") {
                totalPurchases += tx.amount
                if (currentDue == 0L) {
                    lastDueDate = tx.timestamp.toDate()
                }
                currentDue += tx.amount
            } else if (tx.type == "DEPOSIT") {
                totalCollections += tx.amount
                lastPaymentDate = tx.timestamp.toDate()
                
                if (currentDue > 0 && lastDueDate != null) {
                    val daysTaken = (tx.timestamp.toDate().time - lastDueDate.time) / (1000 * 60 * 60 * 24)
                    if (daysTaken > 0) {
                        totalDelayDays += daysTaken
                        paymentCount++
                        if (daysTaken > maxOverdueDays) {
                            maxOverdueDays = daysTaken
                        }
                    }
                }
                
                currentDue -= tx.amount
                if (currentDue <= 0) {
                    currentDue = 0
                    lastDueDate = null
                }
            }
        }
        
        val outstandingDue = customer.totalDue
        
        val avgPaymentTime = if (paymentCount > 0) totalDelayDays / paymentCount else 0L
        
        if (avgPaymentTime > 30) score -= 30
        else if (avgPaymentTime > 15) score -= 20
        else if (avgPaymentTime > 7) score -= 10
        
        if (maxOverdueDays > 60) score -= 30
        else if (maxOverdueDays > 30) score -= 20
        
        if (outstandingDue > 0 && lastDueDate != null) {
            val currentOverdueDays = (System.currentTimeMillis() - lastDueDate.time) / (1000 * 60 * 60 * 24)
            if (currentOverdueDays > 30) score -= 20
            if (currentOverdueDays > maxOverdueDays) maxOverdueDays = currentOverdueDays
        }
        
        if (outstandingDue > customer.creditLimit && customer.creditLimit > 0) {
            score -= 15
        }
        
        if (paymentCount == 0 && outstandingDue > 0) {
            score -= 20
        }
        
        if (paymentCount > 5 && avgPaymentTime <= 3) score += 10
        
        score = score.coerceIn(0, 100)
        
        val riskLevel = when (score) {
            in 95..100 -> RiskLevel.EXCELLENT
            in 80..94 -> RiskLevel.VERY_GOOD
            in 65..79 -> RiskLevel.GOOD
            in 40..64 -> RiskLevel.RISKY
            else -> RiskLevel.HIGH_RISK
        }
        
        if (avgPaymentTime <= 3 && paymentCount > 0) {
            insights.add("Usually pays within 3 days.")
            insights.add("Pays on time.")
        } else if (avgPaymentTime > 15) {
            insights.add("Frequently delays payments.")
        }
        
        if (outstandingDue > 5000) {
            insights.add("Large outstanding balance.")
        }
        
        if (totalPurchases > 10000) {
            insights.add("High-value customer.")
        }
        
        if (insights.isEmpty()) {
            insights.add("New or irregular payment history.")
        }

        return CreditProfile(
            score = score,
            riskLevel = riskLevel,
            averagePaymentTimeDays = avgPaymentTime,
            longestOverdueDays = maxOverdueDays,
            totalPurchases = totalPurchases,
            totalCollections = totalCollections,
            outstandingDue = outstandingDue,
            lastPaymentDate = lastPaymentDate,
            insights = insights
        )
    }
}
