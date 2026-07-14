package com.example

import androidx.lifecycle.ViewModel
import com.example.data.OfflineRepository
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.example.domain.CreditScoreEngine
import com.example.domain.CustomerProfileData
import com.example.domain.CreditProfile

class MainViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    private val _isError = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow("")

    val allCustomerProfiles: StateFlow<List<CustomerProfileData>> = combine(_customers, _transactions) { custs, txs ->
        custs.map { c ->
            val cTxs = txs.filter { it.customerId == c.id }
            CustomerProfileData(c, CreditScoreEngine.calculateProfile(c, cTxs))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _lastSyncTime = MutableStateFlow(System.currentTimeMillis())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val debouncedSearchQuery: Flow<String> = _searchQuery.debounce(300)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val settingsManager = com.example.settings.SettingsManager(application)
    
    private val dashboardStatsUseCase by lazy {
        com.example.domain.DashboardStatsUseCase(
            customerDao = com.example.data.local.AppDatabase.getDatabase(application).customerDao(),
            transactionDao = com.example.data.local.AppDatabase.getDatabase(application).transactionDao(),
            reminderDao = com.example.data.local.AppDatabase.getDatabase(application).reminderDao(),
            settingsManager = settingsManager
        )
    }

    val dashboardUiState: StateFlow<DashboardUiState> = dashboardStatsUseCase
        .getDashboardUiState(_lastSyncTime, debouncedSearchQuery)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

    fun refreshDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1000) // Simulate network refresh since listeners update realtime
            _isLoading.value = false
        }
    }

    fun sendReminder(customer: Customer) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val db = com.example.data.local.AppDatabase.getDatabase(getApplication())
                val reminderDao = db.reminderDao()
                val reminder = com.example.data.local.ReminderEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    customerId = customer.id,
                    customerName = customer.name,
                    method = "SMS",
                    messageText = com.example.utils.MessageUtils.buildReminderMessage(customer),
                    timestamp = System.currentTimeMillis(),
                    status = "PENDING",
                    syncState = "PENDING_ADD"
                )
                reminderDao.insertOrUpdate(reminder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private val _totalDue = MutableStateFlow(0L)
    val totalDue: StateFlow<Long> = _totalDue.asStateFlow()
    
    private val _todayDue = MutableStateFlow(0L)
    val todayDue: StateFlow<Long> = _todayDue.asStateFlow()
    
    private val _todayDeposit = MutableStateFlow(0L)
    val todayDeposit: StateFlow<Long> = _todayDeposit.asStateFlow()

    private val repository = OfflineRepository(application)
    val isOnline = repository.isOnline

    private var dataJob: kotlinx.coroutines.Job? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            fetchData(userId)
        }
    }

    init {
        _isLoading.value = false
        // auth.currentUser can still be null here on a cold start, before Firebase
        // finishes restoring the persisted session. Listening for auth state changes
        // (instead of checking currentUser once) guarantees fetchData() runs as soon
        // as the user is actually available, so the dashboard never gets stuck empty.
        auth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    private suspend fun generateNextCustomerCode(userId: String): String {
        val counterRef = db.collection("users").document(userId).collection("settings").document("counters")
        return try {
            val nextNum = db.runTransaction { transaction ->
                val snapshot = transaction.get(counterRef)
                val current = if (snapshot.exists()) snapshot.getLong("customerCount") ?: 0L else 0L
                val next = current + 1
                transaction.set(counterRef, mapOf("customerCount" to next), SetOptions.merge())
                next
            }.await()
            "CUS-" + String.format("%04d", nextNum)
        } catch (e: Exception) {
            e.printStackTrace()
            "CUS-" + System.currentTimeMillis().toString().takeLast(4)
        }
    }

    private fun migrateCustomersIfNeeded(userId: String, customers: List<Customer>) {
        val needsMigration = customers.filter { it.customerCode.isEmpty() }
        if (needsMigration.isNotEmpty()) {
            viewModelScope.launch {
                needsMigration.forEach { cust ->
                    val code = generateNextCustomerCode(userId)
                    db.collection("users").document(userId).collection("customers").document(cust.id)
                        .update("customerCode", code).await()
                }
            }
        }
    }

    private fun fetchData(userId: String) {
        // Cancel any previous collectors before starting new ones, since the auth
        // listener can fire more than once (e.g. token refresh, re-login).
        dataJob?.cancel()
        dataJob = viewModelScope.launch {
            launch {
                repository.getAllCustomers().collect { list ->
                    _customers.value = list
                    _totalDue.value = list.sumOf { it.totalDue }
                    migrateCustomersIfNeeded(userId, list)
                }
            }

            launch {
                repository.getAllTransactions().collect { list ->
                    _transactions.value = list

                    val todayMillis = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                    var currentTodayDue = 0L
                    var currentTodayDeposit = 0L
                    for (t in list) {
                        if (t.timestamp.toDate().time > todayMillis) {
                            if (t.type == "DUE") {
                                currentTodayDue += t.amount
                            } else {
                                currentTodayDeposit += t.amount
                            }
                        }
                    }
                    _todayDue.value = currentTodayDue
                    _todayDeposit.value = currentTodayDeposit
                }
            }
        }

        // Initial sync when opening
        repository.manualSync()
        _lastSyncTime.value = System.currentTimeMillis()
    }

    suspend fun addCustomer(
        name: String,
        phone: String,
        address: String,
        creditLimit: Long,
        notes: String
    ): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        return repository.addCustomer(name, phone, address, creditLimit, notes) {
            generateNextCustomerCode(userId)
        }
    }



    fun collectPayment(customerId: String, amount: Long, method: String, notes: String) {
        viewModelScope.launch {
            repository.collectPayment(customerId, amount, method, notes)
        }
    }

    fun deleteTransaction(transactionId: String, customerId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId, customerId)
        }
    }

    fun addDue(customer: Customer, amount: Long, notes: String) {
        viewModelScope.launch {
            saveTransaction(
                customer = customer,
                type = "Credit Sale",
                saleAmount = amount,
                paidAmount = 0L,
                remainingDue = amount,
                paymentMethod = "",
                notes = notes,
                imageUri = null,
                transactionDate = System.currentTimeMillis()
            )
        }
    }

    suspend fun saveTransaction(
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
        // Upload image to storage later in background if needed. For now, pass to repository
        return repository.saveTransactionFull(
            customer, type, saleAmount, paidAmount, remainingDue, paymentMethod, notes, imageUri, transactionDate
        )
    }
    

    fun getCustomerProfile(customerId: String): CustomerProfileData? {
        val cust = _customers.value.find { it.id == customerId } ?: return null
        val cTxs = _transactions.value.filter { it.customerId == customerId }
        val profile = CreditScoreEngine.calculateProfile(cust, cTxs)
        return CustomerProfileData(cust, profile)
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun manualSync() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.manualSync()
            _lastSyncTime.value = System.currentTimeMillis()
            delay(1000) // Small delay to show spinner
            _isRefreshing.value = false
        }
    }
}