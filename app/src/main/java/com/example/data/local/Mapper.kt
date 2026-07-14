package com.example.data.local

import com.example.Customer
import com.example.Transaction
import com.example.Reminder
import com.google.firebase.Timestamp
import java.util.Date

object Mapper {
    fun Customer.toEntity(syncState: String = "SYNCED"): CustomerEntity {
        return CustomerEntity(
            id = id,
            customerCode = customerCode,
            name = name,
            phone = phone,
            address = address,
            creditLimit = creditLimit,
            notes = notes,
            totalDue = totalDue,
            totalPaid = totalPaid,
            createdAt = createdAt.toDate().time,
            updatedAt = updatedAt.toDate().time,
            lastTransactionDate = lastTransactionDate.toDate().time,
            syncState = syncState
        )
    }

    fun CustomerEntity.toModel(): Customer {
        return Customer(
            id = id,
            customerCode = customerCode,
            name = name,
            phone = phone,
            address = address,
            creditLimit = creditLimit,
            notes = notes,
            totalDue = totalDue,
            totalPaid = totalPaid,
            createdAt = Timestamp(Date(createdAt)),
            updatedAt = Timestamp(Date(updatedAt)),
            lastTransactionDate = Timestamp(Date(lastTransactionDate))
        )
    }

    fun Transaction.toEntity(syncState: String = "SYNCED"): TransactionEntity {
        return TransactionEntity(
            id = id,
            customerId = customerId,
            customerName = customerName,
            amount = amount,
            type = type,
            timestamp = timestamp.toDate().time,
            paymentMethod = paymentMethod,
            notes = notes,
            saleAmount = saleAmount,
            paidAmount = paidAmount,
            remainingDue = remainingDue,
            receiptImageUrl = receiptImageUrl,
            syncState = syncState
        )
    }

    fun TransactionEntity.toModel(): Transaction {
        return Transaction(
            id = id,
            customerId = customerId,
            customerName = customerName,
            amount = amount,
            type = type,
            timestamp = Timestamp(Date(timestamp)),
            paymentMethod = paymentMethod,
            notes = notes,
            saleAmount = saleAmount,
            paidAmount = paidAmount,
            remainingDue = remainingDue,
            receiptImageUrl = receiptImageUrl
        )
    }

    fun Reminder.toEntity(syncState: String = "SYNCED"): ReminderEntity {
        return ReminderEntity(
            id = id,
            customerId = customerId,
            customerName = customerName,
            method = method,
            messageText = messageText,
            timestamp = timestamp.toDate().time,
            status = status,
            syncState = syncState
        )
    }

    fun ReminderEntity.toModel(): Reminder {
        return Reminder(
            id = id,
            customerId = customerId,
            customerName = customerName,
            method = method,
            messageText = messageText,
            timestamp = Timestamp(Date(timestamp)),
            status = status
        )
    }
}
