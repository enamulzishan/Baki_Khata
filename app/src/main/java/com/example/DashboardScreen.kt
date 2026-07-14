package com.example

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedNegative
import com.example.ui.theme.TextSecondary
import com.example.utils.CurrencyFormatter
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onAddCustomerClick: () -> Unit = {},
    onAddTransaction: (String) -> Unit = {},
    onCustomerClick: (String) -> Unit = {},
    onReportsClick: () -> Unit = {},
    onReminderCenterClick: () -> Unit = {},
    onSeeAllOverdueClick: () -> Unit = {},
    shopViewModel: com.example.shop.ShopViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onMenuClick: () -> Unit = {},
    ownerProfileViewModel: com.example.profile.OwnerProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.dashboardUiState.collectAsState()
    val shopState by shopViewModel.uiState.collectAsState()
    val ownerProfileState by ownerProfileViewModel.uiState.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedSearchQuery = searchQuery
    }

    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Log.e("BakiKhata", "DASHBOARD UI STATE IS: ${uiState::class.simpleName}")
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                DashboardLoadingState()
            }
            is DashboardUiState.Error -> {
                DashboardErrorState(
                    message = state.message,
                    onRetry = { viewModel.refreshDashboard() }
                )
            }
            is DashboardUiState.Empty -> {
                DashboardEmptyState(
                    ownerName = ownerProfileState.profile?.fullName ?: "Owner",
                    onAddCustomerClick = onAddCustomerClick
                )
            }
            is DashboardUiState.Success -> {
                DashboardSuccessState(
                    state = state,
                    shopName = shopState.profile?.shopName ?: "",
                    ownerAvatarUrl = ownerProfileState.profile?.photoUrl,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    debouncedSearchQuery = debouncedSearchQuery,
                    onAddCustomerClick = onAddCustomerClick,
                    onCustomerClick = onCustomerClick,
                    onAddTransaction = onAddTransaction,
                    onReportsClick = onReportsClick,
                    onReminderCenterClick = onReminderCenterClick,
                    onSeeAllOverdueClick = onSeeAllOverdueClick,
                    onMenuClick = onMenuClick,
                    onSendReminder = { viewModel.sendReminder(it) }
                )
            }
        }
    }
}

@Composable
fun DashboardLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = GreenPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Loading your dashboard...", color = TextSecondary)
    }
}

@Composable
fun DashboardErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = RedNegative, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Error loading data", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
            Text("Retry")
        }
    }
}

@Composable
fun DashboardEmptyState(ownerName: String, onAddCustomerClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AccountBalanceWallet,
            contentDescription = "Wallet",
            modifier = Modifier.size(80.dp),
            tint = GreenPrimary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val greeting = when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            else -> "Good Evening"
        }
        Text(
            "$greeting, $ownerName",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "আপনার প্রথম কাস্টমার যোগ করে শুরু করুন",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onAddCustomerClick,
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp)
        ) {
            Text("নতুন কাস্টমার যোগ করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardSuccessState(
    state: DashboardUiState.Success,
    shopName: String,
    ownerAvatarUrl: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    debouncedSearchQuery: String,
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (String) -> Unit,
    onAddTransaction: (String) -> Unit,
    onReportsClick: () -> Unit,
    onReminderCenterClick: () -> Unit,
    onSeeAllOverdueClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSendReminder: (Customer) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top App Bar & Hero Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF076628))
                .padding(bottom = 32.dp)
        ) {
            Column {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!ownerAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ownerAvatarUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { onMenuClick() }
                            )
                        } else {
                                val hash = shopName.hashCode()
                                val colors = listOf(Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFD54F), Color(0xFFBA68C8))
                                val bgColor = colors[kotlin.math.abs(hash) % colors.size]
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .clickable { onMenuClick() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        shopName.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "S",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "বাকি খাতা",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Online", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                    IconButton(onClick = onReminderCenterClick) {
                        BadgedBox(
                            badge = {
                                if (state.unreadNotificationCount > 0) {
                                    Badge { Text(state.unreadNotificationCount.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Reminders", tint = Color.White)
                        }
                    }
                }

                // Search Bar
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Search customers...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    
                    if (debouncedSearchQuery.isNotBlank()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) {
                                if (state.searchResults.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onAddCustomerClick() }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No matches. Add new customer '${debouncedSearchQuery}'?", color = GreenPrimary, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    state.searchResults.forEach { customer ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onCustomerClick(customer.id) }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary)
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(customer.name, fontWeight = FontWeight.Bold)
                                                Text(customer.phone, fontSize = 12.sp, color = TextSecondary)
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFFEEEEEE))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hero Card (Offset upwards)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-24).dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Total Outstanding Due", color = TextSecondary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = CurrencyFormatter.format(state.totalOutstandingDue),
                    color = RedNegative,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFEEEEEE))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Collection", tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Today's Collection", color = TextSecondary, fontSize = 12.sp)
                        }
                        Text(
                            text = CurrencyFormatter.format(state.todayCollection),
                            color = GreenPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Today's New Due", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowUpward, contentDescription = "New Due", tint = RedNegative, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = CurrencyFormatter.format(state.todayNewDue),
                            color = RedNegative,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(
                icon = Icons.Default.PersonAdd,
                label = "Add Cust.",
                color = GreenPrimary,
                onClick = onAddCustomerClick
            )
            QuickActionButton(
                icon = Icons.Default.Assessment,
                label = "Reports",
                color = Color(0xFF2196F3),
                onClick = onReportsClick
            )
            QuickActionButton(
                icon = Icons.Default.Settings,
                label = "Settings",
                color = Color(0xFF607D8B),
                onClick = onMenuClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Overdue Action Items
        if (state.overdueCustomers.isNotEmpty()) {
            Text(
                "Action Required (${state.overdueCustomers.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            state.overdueCustomers.take(3).forEach { customer ->
                OverdueCustomerItem(
                    customer = customer,
                    onClick = { onCustomerClick(customer.id) },
                    onRemind = { onSendReminder(customer) }
                )
            }
            if (state.overdueCustomers.size > 3) {
                TextButton(
                    onClick = onSeeAllOverdueClick,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("See all overdue customers", color = GreenPrimary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recent Transactions
        if (state.recentTransactions.isNotEmpty()) {
            Text(
                "Recent Transactions",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    state.recentTransactions.forEachIndexed { index, tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCustomerClick(tx.customerId) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (tx.type == "DEPOSIT") GreenPrimary.copy(alpha = 0.1f) else RedNegative.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (tx.type == "DEPOSIT") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (tx.type == "DEPOSIT") GreenPrimary else RedNegative,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(tx.customerName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                    Text(
                                        text = if (tx.type == "DEPOSIT") "Payment Received" else "Credit Given",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = CurrencyFormatter.format(tx.amount),
                                fontWeight = FontWeight.Bold,
                                color = if (tx.type == "DEPOSIT") GreenPrimary else RedNegative
                            )
                        }
                        if (index < state.recentTransactions.size - 1) {
                            HorizontalDivider(color = Color(0xFFF0F0F0))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Business Insights
        Text(
            "Business Insights",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InsightCard(
                title = "Success Rate",
                value = "${state.collectionSuccessRate.toInt()}%",
                subtitle = "of all dues collected",
                icon = Icons.Default.DataUsage,
                color = GreenPrimary,
                modifier = Modifier.weight(1f)
            )
            InsightCard(
                title = "Avg. Payment",
                value = if (state.averagePaymentTimeDays >= 0) "${state.averagePaymentTimeDays}d" else "N/A",
                subtitle = if (state.averagePaymentTimeDays >= 0) "average collection time" else "Not enough data",
                icon = Icons.Default.Timer,
                color = Color(0xFFF57C00),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InsightCard(
                title = "Reminders",
                value = "${state.todayReminderCount}",
                subtitle = "pending today",
                icon = Icons.Default.Notifications,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.bestPayingCustomer != null || (state.highestDueCustomer != null && state.highestDueCustomer.totalDue > 0)) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    state.bestPayingCustomer?.let { profile ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = "Best", tint = Color(0xFFFFC107))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Best Payer:", color = TextSecondary)
                            }
                            Text(profile.customer.name, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.bestPayingCustomer != null && state.highestDueCustomer != null && state.highestDueCustomer.totalDue > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
                    }
                    if (state.highestDueCustomer != null && state.highestDueCustomer.totalDue > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "Highest Due", tint = RedNegative)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Highest Due:", color = TextSecondary)
                            }
                            Text(state.highestDueCustomer.name, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // padding for bottom nav
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun OverdueCustomerItem(customer: Customer, onClick: () -> Unit, onRemind: () -> Unit) {
    var isReminding by remember { mutableStateOf(false) }
    
    LaunchedEffect(isReminding) {
        if (isReminding) {
            delay(3000)
            isReminding = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F8)),
        border = androidx.compose.foundation.BorderStroke(1.dp, RedNegative.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Due: ${CurrencyFormatter.format(customer.totalDue)}",
                    color = RedNegative,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = { 
                    if (!isReminding) {
                        isReminding = true
                        onRemind()
                    }
                },
                enabled = !isReminding,
                colors = ButtonDefaults.buttonColors(containerColor = if (isReminding) Color.Gray else RedNegative),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isReminding) "Sent!" else "Remind")
            }
        }
    }
}

@Composable
fun InsightCard(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, color = TextSecondary, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = color)
            Text(subtitle, color = TextSecondary, fontSize = 10.sp)
        }
    }
}
