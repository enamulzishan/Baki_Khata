package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.Customer
import com.example.Reminder
import com.example.data.OfflineRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class ReminderUiState(
    val customers: List<Customer> = emptyList(),
    val filteredCustomers: List<Customer> = emptyList(),
    val selectedCustomerIds: Set<String> = emptySet(),
    val filterType: String = "All", // All, Due Today, 1-7 Days, 8-15 Days, 16-30 Days, 30+ Days
    val sortType: String = "Highest Due", // Highest Due, Lowest Due
    val templates: List<String> = listOf(
        "Hi {customerName}, your due amount is ৳{dueAmount} at {shopName}. Please pay as soon as possible.",
        "হ্যালো {customerName}, আপনার {shopName} এ বকেয়া পরিমাণ ৳{dueAmount}। অনুগ্রহ করে দ্রুত পরিশোধ করুন।"
    ),
    val selectedTemplateIndex: Int = 0,
    val reminderHistory: List<Reminder> = emptyList()
)

class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = OfflineRepository(application)
    
    private val _uiState = MutableStateFlow(ReminderUiState())
    val uiState: StateFlow<ReminderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllCustomers().collect { list ->
                val dueCustomers = list.filter { it.totalDue > 0 }
                _uiState.update { state ->
                    state.copy(customers = dueCustomers).also {
                        applyFiltersAndSorting(it.copy(customers = dueCustomers))
                    }
                }
            }
        }
        
        viewModelScope.launch {
            repository.getAllReminders().collect { reminders ->
                _uiState.update { it.copy(reminderHistory = reminders) }
            }
        }
    }
    
    private fun applyFiltersAndSorting(state: ReminderUiState) {
        val now = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000L
        
        var filtered = state.customers
        
        when (state.filterType) {
            "Due Today" -> filtered = filtered.filter { (now - it.lastTransactionDate.toDate().time) < oneDay }
            "1-7 Days" -> filtered = filtered.filter { 
                val diff = now - it.lastTransactionDate.toDate().time
                diff in oneDay..(7 * oneDay)
            }
            "8-15 Days" -> filtered = filtered.filter { 
                val diff = now - it.lastTransactionDate.toDate().time
                diff in (8 * oneDay)..(15 * oneDay)
            }
            "16-30 Days" -> filtered = filtered.filter { 
                val diff = now - it.lastTransactionDate.toDate().time
                diff in (16 * oneDay)..(30 * oneDay)
            }
            "30+ Days" -> filtered = filtered.filter { 
                val diff = now - it.lastTransactionDate.toDate().time
                diff > 30 * oneDay
            }
        }
        
        when (state.sortType) {
            "Highest Due" -> filtered = filtered.sortedByDescending { it.totalDue }
            "Lowest Due" -> filtered = filtered.sortedBy { it.totalDue }
        }
        
        _uiState.update { it.copy(filteredCustomers = filtered) }
    }

    fun setFilterType(type: String) {
        _uiState.update { it.copy(filterType = type) }
        applyFiltersAndSorting(_uiState.value)
    }

    fun setSortType(type: String) {
        _uiState.update { it.copy(sortType = type) }
        applyFiltersAndSorting(_uiState.value)
    }

    fun toggleSelection(customerId: String) {
        _uiState.update { state ->
            val newSelection = state.selectedCustomerIds.toMutableSet()
            if (newSelection.contains(customerId)) {
                newSelection.remove(customerId)
            } else {
                newSelection.add(customerId)
            }
            state.copy(selectedCustomerIds = newSelection)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedCustomerIds = state.filteredCustomers.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedCustomerIds = emptySet()) }
    }

    fun setSelectedTemplate(index: Int) {
        _uiState.update { it.copy(selectedTemplateIndex = index) }
    }

    fun getProcessedMessage(customer: Customer, shopName: String): String {
        val template = uiState.value.templates[uiState.value.selectedTemplateIndex]
        return template
            .replace("{customerName}", customer.name)
            .replace("{dueAmount}", customer.totalDue.toString())
            .replace("{shopName}", shopName)
    }

    fun recordReminder(customerId: String, customerName: String, method: String, messageText: String) {
        viewModelScope.launch {
            val reminder = Reminder(
                id = UUID.randomUUID().toString(),
                customerId = customerId,
                customerName = customerName,
                method = method,
                messageText = messageText,
                status = "SENT"
            )
            repository.saveReminder(reminder)
        }
    }
}
