package com.example.domain

import com.example.Customer
import com.example.DashboardUiState
import com.example.data.local.CustomerDao
import com.example.data.local.Mapper.toModel
import com.example.data.local.TransactionDao
import com.example.settings.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.Calendar

class DashboardStatsUseCase(
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val reminderDao: com.example.data.local.ReminderDao,
    private val settingsManager: SettingsManager
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getDashboardUiState(lastSyncTimeMillisFlow: Flow<Long>, searchQueryFlow: Flow<String>): Flow<DashboardUiState> {
        return customerDao.getTotalCustomers().flatMapLatest { totalCustomers ->
            if (totalCustomers == 0) {
                flowOf(DashboardUiState.Empty)
            } else {
                buildSuccessState(totalCustomers, lastSyncTimeMillisFlow, searchQueryFlow)
            }
        }
    }

    private fun buildSuccessState(
        totalCustomers: Int,
        lastSyncTimeMillisFlow: Flow<Long>,
        searchQueryFlow: Flow<String>
    ): Flow<DashboardUiState> {
        val todayStart = getStartOfDayMillis()
        
        // 1. Combine direct queries
        val simpleStatsFlow = combine(
            combine(
                customerDao.getTotalOutstandingDue(),
                transactionDao.getCollectionSince(todayStart),
                transactionDao.getNewDueSince(todayStart)
            ) { totalDue, todayCollection, todayNewDue ->
                Triple(totalDue, todayCollection, todayNewDue)
            },
            combine(
                transactionDao.getRecentTransactions(5),
                customerDao.getHighestDueCustomer(),
                reminderDao.getPendingRemindersCountToday(todayStart, todayStart + 24L * 60 * 60 * 1000),
                lastSyncTimeMillisFlow
            ) { recentTxs, highestDueCust, pendingReminders, syncTime ->
                Tuple4(recentTxs, highestDueCust, pendingReminders, syncTime)
            }
        ) { first, second ->
            SimpleStats(
                totalDue = first.first ?: 0L,
                todayCollection = first.second ?: 0L,
                todayNewDue = first.third ?: 0L,
                recentTxs = second.first.map { it.toModel() },
                highestDueCust = second.second?.toModel(),
                pendingRemindersToday = second.third,
                syncTime = second.fourth
            )
        }

        // 2. Combine overdue customers which depends on threshold
        val overdueFlow = settingsManager.overdueThresholdDaysFlow.flatMapLatest { days ->
            val thresholdTime = System.currentTimeMillis() - (days * 24L * 60 * 60 * 1000)
            customerDao.getOverdueCustomers(thresholdTime)
        }

        // 3. Combine heavy processing queries
        val heavyStatsFlow = combine(
            customerDao.getAllActiveCustomersFlow(),
            transactionDao.getAllActiveTransactionsFlow()
        ) { allCusts, allTxs ->
            val customers = allCusts.map { it.toModel() }
            val transactions = allTxs.map { it.toModel() }
            
            var totalDuesEver = 0L
            var totalCollectionsEver = 0L
            transactions.forEach {
                if (it.type == "DUE") totalDuesEver += it.amount
                else if (it.type == "DEPOSIT") totalCollectionsEver += it.amount
            }
            val rawSuccessRate = if (totalDuesEver > 0) (totalCollectionsEver.toFloat() / totalDuesEver.toFloat()) * 100f else 100f
            val successRate = rawSuccessRate.coerceIn(0f, 100f)

            val ninetyDaysAgoDate = java.util.Date(System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000)
            val ninetyDaysAgo = com.google.firebase.Timestamp(ninetyDaysAgoDate)
            
            val recentProfiles = customers.mapNotNull { c ->
                val recentTxs = transactions.filter { it.customerId == c.id && it.timestamp >= ninetyDaysAgo }
                val payments = recentTxs.filter { it.type == "DEPOSIT" }
                if (payments.size >= 2) {
                    val avgTime = CreditScoreEngine.calculateProfile(c, recentTxs).averagePaymentTimeDays
                    Pair(c, avgTime)
                } else null
            }
            val bestPayingPair = recentProfiles.minByOrNull { it.second }
            val bestPaying = if (bestPayingPair != null) {
                CustomerProfileData(bestPayingPair.first, CreditScoreEngine.calculateProfile(bestPayingPair.first, transactions.filter { it.customerId == bestPayingPair.first.id }))
            } else null
            
            val allRecentPayments = transactions.filter { it.type == "DEPOSIT" && it.timestamp >= ninetyDaysAgo }
            val avgPaymentTime = if (allRecentPayments.size >= 3) {
                if (recentProfiles.isNotEmpty()) recentProfiles.sumOf { it.second } / recentProfiles.size else 0L
            } else {
                -1L
            }

            HeavyStats(
                successRate = successRate,
                bestPayingCustomer = bestPaying,
                averagePaymentTimeDays = avgPaymentTime
            )
        }.flowOn(Dispatchers.Default)

        val searchResultsFlow = searchQueryFlow.flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                kotlinx.coroutines.flow.flow {
                    emit(customerDao.searchCustomers(query).map { it.toModel() })
                }
            }
        }

        // 4. Combine all into final state
        return combine(
            simpleStatsFlow,
            overdueFlow,
            heavyStatsFlow,
            searchResultsFlow
        ) { simple, overdueList, heavy, searchResults ->
            DashboardUiState.Success(
                totalOutstandingDue = simple.totalDue,
                totalCustomers = totalCustomers,
                todayCollection = simple.todayCollection,
                todayNewDue = simple.todayNewDue,
                overdueCustomers = overdueList.map { it.toModel() },
                recentTransactions = simple.recentTxs,
                collectionSuccessRate = heavy.successRate,
                highestDueCustomer = simple.highestDueCust,
                bestPayingCustomer = heavy.bestPayingCustomer,
                averagePaymentTimeDays = heavy.averagePaymentTimeDays,
                todayReminderCount = simple.pendingRemindersToday,
                unreadNotificationCount = simple.pendingRemindersToday,
                searchResults = searchResults,
                lastSyncTimeMillis = simple.syncTime
            )
        }
    }

    private fun getStartOfDayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private data class SimpleStats(
        val totalDue: Long,
        val todayCollection: Long,
        val todayNewDue: Long,
        val recentTxs: List<com.example.Transaction>,
        val highestDueCust: Customer?,
        val pendingRemindersToday: Int,
        val syncTime: Long
    )

    private data class HeavyStats(
        val successRate: Float,
        val bestPayingCustomer: CustomerProfileData?,
        val averagePaymentTimeDays: Long
    )
}
