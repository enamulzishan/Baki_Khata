package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE syncState != 'PENDING_DELETE' ORDER BY name ASC")
    fun getAllActiveCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE syncState != 'PENDING_DELETE' ORDER BY name ASC")
    suspend fun getAllActiveCustomers(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: String): CustomerEntity?
    
    @Query("SELECT * FROM customers WHERE syncState IN ('PENDING_ADD', 'PENDING_UPDATE', 'PENDING_DELETE')")
    suspend fun getPendingSyncCustomers(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("SELECT * FROM customers WHERE syncState != 'PENDING_DELETE' AND (name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR customerCode LIKE '%' || :query || '%') ORDER BY name ASC")
    suspend fun searchCustomers(query: String): List<CustomerEntity>

    @Query("SELECT SUM(totalDue) FROM customers WHERE totalDue > 0 AND syncState != 'PENDING_DELETE'")
    fun getTotalOutstandingDue(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM customers WHERE syncState != 'PENDING_DELETE'")
    fun getTotalCustomers(): Flow<Int>

    @Query("SELECT * FROM customers WHERE totalDue > 0 AND lastTransactionDate < :thresholdTime AND syncState != 'PENDING_DELETE' ORDER BY totalDue DESC")
    fun getOverdueCustomers(thresholdTime: Long): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE totalDue > 0 AND syncState != 'PENDING_DELETE' ORDER BY totalDue DESC LIMIT 1")
    fun getHighestDueCustomer(): Flow<CustomerEntity?>

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM customers WHERE syncState = 'SYNCED'")
    suspend fun deleteAllSynced()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' ORDER BY timestamp DESC")
    fun getAllActiveTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId AND syncState != 'PENDING_DELETE' ORDER BY timestamp DESC")
    fun getTransactionsForCustomerFlow(customerId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE syncState IN ('PENDING_ADD', 'PENDING_UPDATE', 'PENDING_DELETE')")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DEPOSIT' AND timestamp >= :startTime AND syncState != 'PENDING_DELETE'")
    fun getCollectionSince(startTime: Long): Flow<Long?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'DUE' AND timestamp >= :startTime AND syncState != 'PENDING_DELETE'")
    fun getNewDueSince(startTime: Long): Flow<Long?>

    @Query("SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM transactions WHERE syncState = 'SYNCED'")
    suspend fun deleteAllSynced()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE syncState != 'PENDING_DELETE' ORDER BY timestamp DESC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getRemindersForCustomer(customerId: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE syncState IN ('PENDING_ADD', 'PENDING_UPDATE', 'PENDING_DELETE')")
    suspend fun getPendingSyncReminders(): List<ReminderEntity>
    
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT COUNT(*) FROM reminders WHERE status = 'PENDING' AND timestamp >= :todayStart AND timestamp < :todayEnd")
    fun getPendingRemindersCountToday(todayStart: Long, todayEnd: Long): Flow<Int>
}
