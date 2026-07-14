package com.example.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Customer
import com.example.MainViewModel
import com.example.Transaction
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBorder
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedNegative
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.*

enum class DateRange { TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH, LAST_MONTH, ALL_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var selectedRange by remember { mutableStateOf(DateRange.THIS_MONTH) }
    var showRangeDropdown by remember { mutableStateOf(false) }
    var currentReportTab by remember { mutableStateOf(0) }

    val context = LocalContext.current

    // Filter Logic
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val todayStart = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStart = calendar.timeInMillis
    val yesterdayEnd = todayStart - 1

    calendar.timeInMillis = todayStart
    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
    val thisWeekStart = calendar.timeInMillis

    calendar.timeInMillis = todayStart
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val thisMonthStart = calendar.timeInMillis

    calendar.add(Calendar.MONTH, -1)
    val lastMonthStart = calendar.timeInMillis
    calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    val lastMonthEnd = calendar.timeInMillis

    val filteredTransactions = remember(selectedRange, transactions) {
        transactions.filter { tx ->
            val t = tx.timestamp.toDate().time
            when (selectedRange) {
                DateRange.TODAY -> t >= todayStart
                DateRange.YESTERDAY -> t in yesterdayStart..yesterdayEnd
                DateRange.THIS_WEEK -> t >= thisWeekStart
                DateRange.THIS_MONTH -> t >= thisMonthStart
                DateRange.LAST_MONTH -> t in lastMonthStart..lastMonthEnd
                DateRange.ALL_TIME -> true
            }
        }
    }

    val totalSales = filteredTransactions.filter { it.type == "DUE" }.sumOf { it.amount }
    val totalCollections = filteredTransactions.filter { it.type == "DEPOSIT" }.sumOf { it.amount }
    val newDueCreated = totalSales - totalCollections // Roughly

    val totalOutstandingDue = customers.sumOf { it.totalDue }
    val totalActiveCustomers = customers.count { it.totalDue > 0 || it.totalPaid > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics", fontWeight = FontWeight.Bold, color = GreenPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val pdf = ExportUtils.generatePdf(context, customers, filteredTransactions, selectedRange.name, totalSales, totalCollections)
                        if (pdf != null) ExportUtils.shareFile(context, pdf, "application/pdf")
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = GreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date Range Selector
            Box {
                OutlinedButton(
                    onClick = { showRangeDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(selectedRange.name.replace("_", " "), modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                }
                DropdownMenu(expanded = showRangeDropdown, onDismissRequest = { showRangeDropdown = false }) {
                    DateRange.values().forEach { range ->
                        DropdownMenuItem(
                            text = { Text(range.name.replace("_", " ")) },
                            onClick = { selectedRange = range; showRangeDropdown = false }
                        )
                    }
                }
            }

            // Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCard(title = "Total Sales", amount = totalSales, modifier = Modifier.weight(1f), isPositive = true)
                SummaryCard(title = "Collections", amount = totalCollections, modifier = Modifier.weight(1f), isPositive = true)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCard(title = "Total Outstanding", amount = totalOutstandingDue, modifier = Modifier.weight(1f), isPositive = false)
                SummaryCard(title = "New Due Created", amount = newDueCreated.coerceAtLeast(0), modifier = Modifier.weight(1f), isPositive = false)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryCardSmall(title = "Transactions", count = filteredTransactions.size.toLong(), modifier = Modifier.weight(1f))
                SummaryCardSmall(title = "Active Customers", count = totalActiveCustomers.toLong(), modifier = Modifier.weight(1f))
            }

            // Charts
            Text("Sales vs Collections Trend", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            TrendChart(transactions = filteredTransactions)

            // Top Due Customers
            Text("Top Due Customers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            val topDue = customers.sortedByDescending { it.totalDue }.take(5)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topDue.forEach { cust ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cust.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text("৳${cust.totalDue}", fontWeight = FontWeight.Bold, color = RedNegative)
                    }
                }
            }
            
            TabRow(selectedTabIndex = currentReportTab, containerColor = BackgroundColor, contentColor = GreenPrimary) {
                Tab(selected = currentReportTab == 0, onClick = { currentReportTab = 0 }, text = { Text("Customer Report") })
                Tab(selected = currentReportTab == 1, onClick = { currentReportTab = 1 }, text = { Text("Payment Report") })
            }
            
            if (filteredTransactions.isEmpty() && customers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("No report data available.", color = TextSecondary, fontSize = 16.sp)
                }
            } else if (currentReportTab == 0) {
                // Customer Report
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customers.filter { it.totalDue > 0 || it.totalPaid > 0 }.forEach { cust ->
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(cust.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Sales: ৳${filteredTransactions.filter { it.customerId == cust.id && it.type == "DUE" }.sumOf { it.amount }}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Due: ৳${cust.totalDue}", fontWeight = FontWeight.Bold, color = RedNegative)
                                Text("Col: ৳${filteredTransactions.filter { it.customerId == cust.id && it.type == "DEPOSIT" }.sumOf { it.amount }}", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            } else {
                // Payment Report
                val rptFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredTransactions.filter { it.type == "DEPOSIT" }.forEach { tx ->
                        val txDate = rptFormat.format(tx.timestamp.toDate())
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.White).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(tx.customerName, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("$txDate • via ${tx.paymentMethod}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text("৳${tx.amount}", fontWeight = FontWeight.Bold, color = GreenPrimary)
                        }
                    }
                }
            }
            // Generate Excel/CSV
            Button(
                onClick = {
                    val csv = ExportUtils.generateCsv(context, filteredTransactions)
                    if (csv != null) ExportUtils.shareFile(context, csv, "text/csv")
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Export Excel (CSV)", fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: Long, modifier: Modifier = Modifier, isPositive: Boolean) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = TextSecondary)
            Text("৳$amount", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isPositive) GreenPrimary else RedNegative)
        }
    }
}

@Composable
fun SummaryCardSmall(title: String, count: Long, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontSize = 12.sp, color = TextSecondary)
            Text("$count", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun TrendChart(transactions: List<Transaction>) {
    val df = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }
    // Simple bar chart logic
    val sales = transactions.filter { it.type == "DUE" }.groupBy { df.format(it.timestamp.toDate()) }.mapValues { it.value.sumOf { tx -> tx.amount } }
    val collections = transactions.filter { it.type == "DEPOSIT" }.groupBy { df.format(it.timestamp.toDate()) }.mapValues { it.value.sumOf { tx -> tx.amount } }
    
    val allDates = (sales.keys + collections.keys).distinct().sorted()
    if (allDates.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(Color.White, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Text("No data to show chart", color = TextSecondary)
        }
        return
    }

    val maxAmount = (sales.values.maxOrNull() ?: 0L).coerceAtLeast(collections.values.maxOrNull() ?: 0L)
    
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barWidth = width / (allDates.size * 3).coerceAtLeast(1)
            
            allDates.forEachIndexed { index, date ->
                val sAmount = sales[date] ?: 0L
                val cAmount = collections[date] ?: 0L
                
                val sHeight = if (maxAmount > 0) (sAmount.toFloat() / maxAmount) * height else 0f
                val cHeight = if (maxAmount > 0) (cAmount.toFloat() / maxAmount) * height else 0f
                
                val startX = index * (barWidth * 3)
                
                // Draw Sales Bar
                drawRect(
                    color = RedNegative.copy(alpha = 0.7f),
                    topLeft = Offset(startX, height - sHeight),
                    size = Size(barWidth, sHeight)
                )
                
                // Draw Collection Bar
                drawRect(
                    color = GreenPrimary.copy(alpha = 0.7f),
                    topLeft = Offset(startX + barWidth, height - cHeight),
                    size = Size(barWidth, cHeight)
                )
            }
        }
    }
}
