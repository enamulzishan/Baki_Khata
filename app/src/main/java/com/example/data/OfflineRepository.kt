package com.example.data

import android.content.Context
import android.net.Uri
import com.example.Customer
import com.example.Transaction
import com.example.Reminder
import com.example.data.local.AppDatabase
import com.example.data.local.Mapper.toEntity
import com.example.data.local.Mapper.toModel
import com.example.data.sync.NetworkMonitor
import com.example.data.sync.SyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OfflineRepository(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val customerDao = db.customerDao()
    private val transactionDao = db.transactionDao()
    private val reminderDao = db.reminderDao()
    private val syncManager = SyncManager(context)
    
    val networkMonitor = NetworkMonitor(context)
    val isOnline = networkMonitor.isOnline

    fun getAllCustomers(): Flow<List<Customer>> {
        return customerDao.getAllActiveCustomersFlow().map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllActiveTransactionsFlow().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addCustomer(
        name: String,
        phone: String,
        address: String,
        creditLimit: Long,
        notes: String,
        generateCode: suspend () -> String
    ): Boolean {
        return try {
            val code = generateCode()
            val id = UUID.randomUUID().toString()
            val customer = Customer(
                id = id,
                customerCode = code,
                name = name,
                phone = phone,
                address = address,
                creditLimit = creditLimit,
                notes = notes,
                totalDue = 0L,
                totalPaid = 0L
            )
            customerDao.insertOrUpdate(customer.toEntity("PENDING_ADD"))
            syncManager.enqueueSync()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun updateCustomer(customer: Customer) {
        val existing = customerDao.getCustomerById(customer.id)
        val state = if (existing?.syncState == "PENDING_ADD") "PENDING_ADD" else "PENDING_UPDATE"
        customerDao.insertOrUpdate(customer.toEntity(state))
        syncManager.enqueueSync()
    }
    


    suspend fun saveTransactionFull(
        customer: Customer,
        type: String,
        saleAmount: Long,
        paidAmount: Long,
        remainingDue: Long,
        paymentMethod: String,
        notes: String,
        imageUri: Uri?,
        transactionDate: Long
    ): String? {
        return try {
            val actualType = if (type == "Credit Sale") "DUE" else "DEPOSIT"
            val txId = UUID.randomUUID().toString()
            
            // Note: we can't upload receipt image offline directly to Storage. We should enqueue it or skip image for offline.
            // For simplicity, we just save local URI as receiptImageUrl and later upload it, or ignore image offline upload logic for now.
            val invNum = "INV-" + java.text.SimpleDateFormat("yyyy").format(java.util.Date()) + "-" + String.format("%06d", (Math.random() * 999999).toInt())
            val transaction = Transaction(
                id = txId,
                customerId = customer.id,
                customerName = customer.name,
                amount = if (actualType == "DUE") saleAmount else paidAmount,
                type = actualType,
                timestamp = com.google.firebase.Timestamp(java.util.Date(transactionDate)),
                paymentMethod = paymentMethod,
                notes = notes,
                saleAmount = saleAmount,
                paidAmount = paidAmount,
                remainingDue = remainingDue,
                receiptImageUrl = imageUri?.toString(),
                invoiceNumber = invNum
            )
            
            var newDue = customer.totalDue
            var newPaid = customer.totalPaid
            if (actualType == "DUE") {
                newDue += remainingDue
                newPaid += paidAmount
            } else {
                newDue -= paidAmount
                newPaid += paidAmount
            }
            if (newDue < 0) newDue = 0
            
            val updatedCust = customerDao.getCustomerById(customer.id)
            if (updatedCust != null) {
                val state = if (updatedCust.syncState == "PENDING_ADD") "PENDING_ADD" else "PENDING_UPDATE"
                customerDao.insertOrUpdate(updatedCust.copy(
                    totalDue = newDue,
                    totalPaid = newPaid,
                    lastTransactionDate = transactionDate,
                    updatedAt = System.currentTimeMillis(),
                    syncState = state
                ))
            }
            
            transactionDao.insertOrUpdate(transaction.toEntity("PENDING_ADD"))
            syncManager.enqueueSync()
            txId
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun collectPayment(customerId: String, amount: Long, method: String, notes: String) {
        val cust = customerDao.getCustomerById(customerId) ?: return
        val currentDue = cust.totalDue
        val currentPaid = cust.totalPaid
        
        val updatedDue = currentDue - amount
        val updatedPaid = currentPaid + amount
        
        val state = if (cust.syncState == "PENDING_ADD") "PENDING_ADD" else "PENDING_UPDATE"
        customerDao.insertOrUpdate(cust.copy(
            totalDue = updatedDue,
            totalPaid = updatedPaid,
            updatedAt = System.currentTimeMillis(),
            syncState = state
        ))
        
        val txId = UUID.randomUUID().toString()
        val transaction = Transaction(
            id = txId,
            customerId = customerId,
            customerName = cust.name,
            amount = amount,
            type = "DEPOSIT",
            paymentMethod = method,
            notes = notes
        )
        transactionDao.insertOrUpdate(transaction.toEntity("PENDING_ADD"))
        syncManager.enqueueSync()
    }
    
    suspend fun deleteTransaction(transactionId: String, customerId: String) {
        val tx = transactionDao.getTransactionById(transactionId) ?: return
        val customer = customerDao.getCustomerById(customerId) ?: return
        
        var newDue = customer.totalDue
        var newPaid = customer.totalPaid
        
        if (tx.type == "DUE") {
            newDue -= tx.amount
        } else if (tx.type == "DEPOSIT") {
            newDue += tx.amount
            newPaid -= tx.amount
        }
        
        val state = if (customer.syncState == "PENDING_ADD") "PENDING_ADD" else "PENDING_UPDATE"
        customerDao.insertOrUpdate(customer.copy(
            totalDue = newDue,
            totalPaid = newPaid,
            updatedAt = System.currentTimeMillis(),
            syncState = state
        ))
        
        transactionDao.insertOrUpdate(tx.copy(syncState = "PENDING_DELETE"))
        syncManager.enqueueSync()
    }
    
    fun manualSync() {
        syncManager.enqueueSync()
    }

    fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllRemindersFlow().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun saveReminder(reminder: Reminder) {
        val entity = reminder.toEntity(syncState = "PENDING_ADD")
        reminderDao.insertOrUpdate(entity)
        syncManager.enqueueSync()
    }
}
