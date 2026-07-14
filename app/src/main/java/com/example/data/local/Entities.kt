package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "customers", indices = [androidx.room.Index(value = ["syncState"])])
data class CustomerEntity(
    @PrimaryKey val id: String,
    val customerCode: String,
    val name: String,
    val phone: String,
    val address: String,
    val creditLimit: Long,
    val notes: String,
    val totalDue: Long,
    val totalPaid: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val lastTransactionDate: Long,
    val syncState: String // SYNCED, PENDING_ADD, PENDING_UPDATE, PENDING_DELETE
)

@Entity(tableName = "transactions", indices = [androidx.room.Index(value = ["customerId"]), androidx.room.Index(value = ["syncState"])])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val amount: Long,
    val type: String,
    val timestamp: Long,
    val paymentMethod: String,
    val notes: String,
    val saleAmount: Long,
    val paidAmount: Long,
    val remainingDue: Long,
    val receiptImageUrl: String?,
    val syncState: String
)

@Entity(tableName = "reminders", indices = [androidx.room.Index(value = ["customerId"]), androidx.room.Index(value = ["syncState"])])
data class ReminderEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val customerName: String,
    val method: String, // SMS, WHATSAPP
    val messageText: String,
    val timestamp: Long,
    val status: String,
    val syncState: String
)
