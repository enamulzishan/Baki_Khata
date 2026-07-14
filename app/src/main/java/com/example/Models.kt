package com.example

import androidx.compose.runtime.Stable
import com.google.firebase.Timestamp

@Stable
data class Customer(
    val id: String = "",
    val customerCode: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val creditLimit: Long = 0,
    val notes: String = "",
    val totalDue: Long = 0,
    val totalPaid: Long = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val lastTransactionDate: Timestamp = Timestamp.now()
)

@Stable
data class Transaction(
    val id: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val amount: Long = 0,
    val type: String = "DUE",
    val timestamp: Timestamp = Timestamp.now(),
    val paymentMethod: String = "",
    val notes: String = "",
    val saleAmount: Long = 0,
    val paidAmount: Long = 0,
    val remainingDue: Long = 0,
    val receiptImageUrl: String? = null,
    val invoiceNumber: String? = null
)
