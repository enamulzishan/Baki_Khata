package com.example.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.local.Mapper.toModel
import com.example.data.local.Mapper.toEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val auth = FirebaseAuth.getInstance()
        val uid = auth.currentUser?.uid ?: return Result.success() // Can't sync if not logged in
        
        val db = AppDatabase.getDatabase(applicationContext)
        val customerDao = db.customerDao()
        val transactionDao = db.transactionDao()
        val firestore = FirebaseFirestore.getInstance()

        try {
            // 1. Sync Customers (Up)
            val pendingCustomers = customerDao.getPendingSyncCustomers()
            for (entity in pendingCustomers) {
                if (entity.syncState == "PENDING_DELETE") {
                    firestore.collection("users").document(uid)
                        .collection("customers").document(entity.id).delete().await()
                    customerDao.deleteById(entity.id)
                } else {
                    val model = entity.toModel()
                    firestore.collection("users").document(uid)
                        .collection("customers").document(entity.id).set(model).await()
                    customerDao.insertOrUpdate(entity.copy(syncState = "SYNCED"))
                }
            }

            // 2. Sync Transactions (Up)
            val pendingTransactions = transactionDao.getPendingSyncTransactions()
            for (entity in pendingTransactions) {
                if (entity.syncState == "PENDING_DELETE") {
                    firestore.collection("users").document(uid)
                        .collection("transactions").document(entity.id).delete().await()
                    transactionDao.deleteById(entity.id)
                } else {
                    val model = entity.toModel()
                    firestore.collection("users").document(uid)
                        .collection("transactions").document(entity.id).set(model).await()
                    transactionDao.insertOrUpdate(entity.copy(syncState = "SYNCED"))
                }
            }
            
            // 3. Sync Down (Simple full refresh of changed records since we don't have a specific offline first trigger)
            // Note: A true offline-first would do a complex merge. Here we fetch all and merge newer.
            val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val lastSyncTime = prefs.getLong("last_sync_time_$uid", 0L)
            val lastSyncTimestamp = com.google.firebase.Timestamp(java.util.Date(lastSyncTime))
            
            val remoteCustomers = firestore.collection("users").document(uid)
                .collection("customers")
                .whereGreaterThanOrEqualTo("updatedAt", lastSyncTimestamp)
                .get().await().toObjects(com.example.Customer::class.java)
            
            for (remote in remoteCustomers) {
                val local = customerDao.getCustomerById(remote.id)
                if (local == null || local.syncState == "SYNCED") {
                    customerDao.insertOrUpdate(remote.toEntity("SYNCED"))
                } else {
                    // Conflict: if remote is newer
                    if (remote.updatedAt.toDate().time > local.updatedAt) {
                        customerDao.insertOrUpdate(remote.toEntity("SYNCED"))
                    }
                }
            }

            val remoteTransactions = firestore.collection("users").document(uid)
                .collection("transactions")
                .whereGreaterThanOrEqualTo("timestamp", lastSyncTimestamp)
                .get().await().toObjects(com.example.Transaction::class.java)
            
            for (remote in remoteTransactions) {
                transactionDao.insertOrUpdate(remote.toEntity("SYNCED")) // simpler for transactions
            }

            prefs.edit().putLong("last_sync_time_$uid", System.currentTimeMillis()).apply()
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error syncing", e)
            return Result.retry()
        }
    }
}
