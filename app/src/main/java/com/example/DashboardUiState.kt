package com.example

import com.example.domain.CustomerProfileData

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    object Empty : DashboardUiState
    data class Error(val message: String) : DashboardUiState
    data class Success(
        val totalOutstandingDue: Long,
        val totalCustomers: Int,
        val todayCollection: Long,
        val todayNewDue: Long,
        val overdueCustomers: List<Customer>,
        val recentTransactions: List<Transaction>,
        val collectionSuccessRate: Float,
        val highestDueCustomer: Customer?,
        val bestPayingCustomer: CustomerProfileData?,
        val averagePaymentTimeDays: Long,
        val todayReminderCount: Int,
        val unreadNotificationCount: Int,
        val searchResults: List<Customer> = emptyList(),
        val lastSyncTimeMillis: Long
    ) : DashboardUiState
}
