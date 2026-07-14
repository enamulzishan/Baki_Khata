package com.example

import androidx.activity.compose.BackHandler

import com.example.invoice.InvoiceReceiptScreen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.clickable

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Message
import android.os.Bundle
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.data.sync.DailyReminderWorker
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    
    // Explicitly enable Firestore offline persistence
    val firestore = FirebaseFirestore.getInstance()
    firestore.firestoreSettings = firestoreSettings {
        setLocalCacheSettings(persistentCacheSettings {})
    }
    
    val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(1, TimeUnit.MINUTES) // Just for demonstration, normally would schedule for a specific time
        .build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "daily_reminder",
        androidx.work.ExistingPeriodicWorkPolicy.KEEP,
        dailyWorkRequest
    )
    
    setContent {
      MyApplicationTheme {
        var isAuthenticated by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
        
        if (isAuthenticated) {
            var showAddCustomerDialog by remember { mutableStateOf(false) }
            val viewModel: MainViewModel = viewModel()
            val settingsViewModel: com.example.settings.SettingsViewModel = viewModel()
            val settingsState by settingsViewModel.uiState.collectAsState()
            var isUnlocked by remember { mutableStateOf(false) }
            
            if (!isUnlocked) {
                PasswordLockScreen(
                    email = FirebaseAuth.getInstance().currentUser?.email ?: "",
                    onUnlocked = { isUnlocked = true },
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        isAuthenticated = false
                    }
                )
                return@MyApplicationTheme
            }

            var currentTab by remember { mutableStateOf("home") }
            var selectedCustomerId by remember { mutableStateOf<String?>(null) }
            val isOnline by viewModel.isOnline.collectAsState(initial = true)
            
            var showAddTransactionScreen by remember { mutableStateOf(false) }
            var invoiceTransactionId by remember { mutableStateOf<String?>(null) }
            var transactionType by remember { mutableStateOf("Credit Sale") }
            var currentCustomerFilter by remember { mutableStateOf<String?>(null) }


            if (showAddTransactionScreen) {
                BackHandler { showAddTransactionScreen = false }
            } else if (invoiceTransactionId != null) {
                BackHandler { invoiceTransactionId = null }
            } else if (selectedCustomerId != null) {
                BackHandler { selectedCustomerId = null }
            } else if (currentTab != "home") {
                BackHandler { currentTab = "home" }
            }

            if (showAddCustomerDialog) {
                AddCustomerDialog(
                    viewModel = viewModel,
                    onDismiss = { showAddCustomerDialog = false },
                    onSuccess = { showAddCustomerDialog = false }
                )
            }

            if (invoiceTransactionId != null) {
                InvoiceReceiptScreen(
                    viewModel = viewModel,
                    transactionId = invoiceTransactionId!!,
                    onBack = { invoiceTransactionId = null }
                )
            } else if (selectedCustomerId != null) {
                CustomerDetailScreen(
                    viewModel = viewModel,
                    customerId = selectedCustomerId!!,
                    onBack = { selectedCustomerId = null },
                    onInvoiceClick = { id -> invoiceTransactionId = id }
                )
            } else if (showAddTransactionScreen) {
                AddTransactionScreen(
                    viewModel = viewModel,
                    onBack = { showAddTransactionScreen = false },
                    onTransactionSaved = { id -> 
                        showAddTransactionScreen = false
                        invoiceTransactionId = id 
                    },
                    preselectedType = transactionType
                )
            } else {
                var showBottomSheet by remember { mutableStateOf(false) }
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val ownerProfileViewModel: com.example.profile.OwnerProfileViewModel = viewModel()
                val ownerProfileState by ownerProfileViewModel.uiState.collectAsState()
                
                ModalNavigationDrawer(
                    modifier = Modifier.fillMaxSize(),
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            // Drawer Header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GreenPrimary)
                                    .padding(vertical = 32.dp, horizontal = 16.dp)
                            ) {
                                Column {
                                    val profile = ownerProfileState.profile
                                    if (profile != null && profile.photoUrl.isNotEmpty()) {
                                        coil.compose.AsyncImage(
                                            model = profile.photoUrl,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = profile?.fullName?.takeIf { it.isNotBlank() } ?: "Owner Name",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = profile?.email?.takeIf { it.isNotBlank() } ?: "Email Address",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                    if (profile != null && profile.phone.isNotBlank()) {
                                        Text(
                                            text = profile.phone,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { currentTab = "my_profile"; scope.launch { drawerState.close() } },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                    ) {
                                        Text("Edit Profile", fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val drawerItems = listOf(
                                Triple("home", "Home", Icons.Default.Home),
                                Triple("customers", "Customers", Icons.Default.People),
                                Triple("transactions", "Transactions", Icons.AutoMirrored.Filled.List),
                                Triple("reports", "Reports", Icons.Default.BarChart),
                                Triple("reminder_center", "Reminder Center", Icons.Default.Notifications),
                                Triple("shop_profile", "Shop Profile", Icons.Default.Store),
                                Triple("my_profile", "My Profile", Icons.Default.Person),
                                Triple("settings", "Settings", Icons.Default.Settings)
                            )
                            
                            LazyColumn {
                                items(drawerItems) { (tabId, label, icon) ->
                                    NavigationDrawerItem(
                                        icon = { Icon(icon, contentDescription = null) },
                                        label = { Text(label) },
                                        selected = currentTab == tabId,
                                        onClick = {
                                            currentTab = tabId
                                            scope.launch { drawerState.close() }
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                                item {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    NavigationDrawerItem(
                                        icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                                        label = { Text("Logout") },
                                        selected = false,
                                        onClick = {
                                            FirebaseAuth.getInstance().signOut()
                                            isAuthenticated = false
                                        },
                                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                    )
                                }
                            }
                        }
                    }
                ) {

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = { BottomNavBar(currentTab, { currentTab = it }, { showBottomSheet = true }) },

                    topBar = {
                        if (!isOnline) {
                            Box(modifier = Modifier.fillMaxWidth().background(RedNegative).padding(4.dp), contentAlignment = Alignment.Center) {
                                Text("🔴 Offline - Changes will sync when online", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                ) { innerPadding ->
                    if (showBottomSheet) {
                        @OptIn(ExperimentalMaterial3Api::class)
                        ModalBottomSheet(
                            onDismissRequest = { showBottomSheet = false },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Quick Actions", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                                ListItem(
                                    headlineContent = { Text("নতুন গ্রাহক") },
                                    leadingContent = { Icon(Icons.Default.PersonAdd, contentDescription = null, tint = GreenPrimary) },
                                    modifier = Modifier.clickable { showBottomSheet = false; showAddCustomerDialog = true }
                                )
                                ListItem(
                                    headlineContent = { Text("নতুন লেনদেন") },
                                    leadingContent = { Icon(Icons.Default.Receipt, contentDescription = null, tint = GreenPrimary) },
                                    modifier = Modifier.clickable { showBottomSheet = false; transactionType = "Credit Sale"; showAddTransactionScreen = true }
                                )
                                ListItem(
                                    headlineContent = { Text("আদায় করুন") },
                                    leadingContent = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = GreenPrimary) },
                                    modifier = Modifier.clickable { showBottomSheet = false; transactionType = "Payment Collection"; showAddTransactionScreen = true }
                                )
                            }
                        }
                    }
                    when (currentTab) {
                        "home" -> DashboardScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding),
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onAddCustomerClick = { showAddCustomerDialog = true },
                            onAddTransaction = { type: String ->
                                transactionType = type
                                showAddTransactionScreen = true
                            },
                            onCustomerClick = { customerId: String ->
                                selectedCustomerId = customerId
                            },
                            onReportsClick = { currentTab = "reports" },
                            onReminderCenterClick = { currentTab = "reminder_center" },
                            onSeeAllOverdueClick = {
                                currentCustomerFilter = "overdue"
                                currentTab = "customer"
                            }
                        )
                        "reports" -> com.example.reports.ReportsScreen(
                            viewModel = viewModel,
                            onBack = { currentTab = "home" }
                        )
                        "customer" -> CustomerListScreen(
                            viewModel = viewModel,
                            initialFilter = currentCustomerFilter,
                            modifier = Modifier.padding(innerPadding),
                            onCustomerClick = { selectedCustomerId = it }
                        )
                        "settings" -> com.example.settings.SettingsScreen(
                            onBack = { currentTab = "home" },
                            onLogout = {
                                isAuthenticated = false
                                currentTab = "home"
                            },
                            onShopProfileClick = { currentTab = "shop_profile" }
                        )
                        "shop_profile" -> com.example.shop.ShopProfileScreen(
                            onBack = { currentTab = "settings" }
                        )
                        "my_profile" -> com.example.profile.OwnerProfileScreen(
                            onBack = { currentTab = "home" }
                        )
                        else -> Box(modifier = Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.Center) { Text("Coming Soon") }
                    }
                }
            }
                }
        } else {
            AuthScreen(onAuthSuccess = { isAuthenticated = true })
        }
      }
    }
  }
}



@Composable
fun CustomerRowWithBadge(data: com.example.domain.CustomerProfileData, overdueThreshold: Int, onClick: (String) -> Unit) {
    val context = LocalContext.current
    val firstLetter = data.customer.name.take(1).uppercase()
    val colors = listOf(Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), Color(0xFFFFB74D), Color(0xFF9575CD), Color(0xFF4DB6AC))
    val avatarColor = remember(data.customer.id) { colors[Math.abs(data.customer.name.hashCode()) % colors.size] }
    
    val daysSinceLastTx = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - data.customer.lastTransactionDate.toDate().time)
    val dueColor = when {
        data.customer.totalDue == 0L -> GreenPrimary
        daysSinceLastTx <= overdueThreshold -> Color(0xFFFFA000) // Amber
        else -> RedNegative // Red
    }
    val relativeTime = com.example.utils.TimeUtils.getRelativeTime(data.customer.lastTransactionDate.toDate().time)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick(data.customer.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(firstLetter, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    if (data.customer.syncState != "SYNCED") {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Pending Sync",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp).align(Alignment.BottomEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape).padding(2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(data.customer.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("৳${com.example.utils.CurrencyFormatter.format(data.customer.totalDue)}", fontWeight = FontWeight.Bold, color = dueColor)
                        Text(" • $relativeTime", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${data.customer.phone}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(36.dp).background(BackgroundColor, CircleShape)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = GreenPrimary, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = {
                        val message = com.example.utils.MessageUtils.buildReminderMessage(data.customer)
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            setData(Uri.parse("smsto:${data.customer.phone}"))
                            putExtra("sms_body", message)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(36.dp).background(BackgroundColor, CircleShape)
                ) {
                    Icon(Icons.Default.Message, contentDescription = "Message", tint = GreenPrimary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(viewModel: MainViewModel, modifier: Modifier = Modifier, initialFilter: String? = null, onCustomerClick: (String) -> Unit = {}) {
    val profiles by viewModel.allCustomerProfiles.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val overdueThreshold by viewModel.settingsManager.overdueThresholdDaysFlow.collectAsState(initial = 15)
    
    var searchQuery by remember { mutableStateOf("") }
    var currentFilter by remember { mutableStateOf(initialFilter?.replaceFirstChar { it.uppercase() } ?: "All") }
    var sortOption by remember { mutableStateOf("Default") }
    var showSortMenu by remember { mutableStateOf(false) }

    val filteredProfiles = remember(profiles, searchQuery, currentFilter, overdueThreshold) {
        val searchFiltered = profiles.filter {
            it.customer.name.contains(searchQuery, ignoreCase = true) ||
            it.customer.phone.contains(searchQuery, ignoreCase = true) ||
            it.customer.customerCode.contains(searchQuery, ignoreCase = true)
        }
        val now = System.currentTimeMillis()
        searchFiltered.filter { data ->
            when (currentFilter) {
                "Due" -> data.customer.totalDue > 0
                "Overdue" -> {
                    val daysSinceTx = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(now - data.customer.lastTransactionDate.toDate().time)
                    data.customer.totalDue > 0 && daysSinceTx > overdueThreshold
                }
                "Paid off" -> data.customer.totalDue == 0L
                else -> true
            }
        }
    }
    
    val sortedProfiles = remember(filteredProfiles, sortOption) {
        when (sortOption) {
            "Name (A-Z)" -> filteredProfiles.sortedBy { it.customer.name }
            "Due (High->Low)" -> filteredProfiles.sortedByDescending { it.customer.totalDue }
            "Last Tx (Oldest Overdue)" -> filteredProfiles.sortedBy { it.customer.lastTransactionDate.toDate().time }
            else -> filteredProfiles
        }
    }

    Column(modifier = modifier.fillMaxSize().background(BackgroundColor).padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("কাস্টমার তালিকা", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    DropdownMenuItem(text = { Text("Sort: Default") }, onClick = { sortOption = "Default"; showSortMenu = false })
                    DropdownMenuItem(text = { Text("Sort: Name (A-Z)") }, onClick = { sortOption = "Name (A-Z)"; showSortMenu = false })
                    DropdownMenuItem(text = { Text("Sort: Due (High->Low)") }, onClick = { sortOption = "Due (High->Low)"; showSortMenu = false })
                    DropdownMenuItem(text = { Text("Sort: Last Tx (Oldest Overdue)") }, onClick = { sortOption = "Last Tx (Oldest Overdue)"; showSortMenu = false })
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("নাম, ফোন বা কোড দিয়ে খুঁজুন") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GreenPrimary,
                unfocusedBorderColor = CardBorder
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Due", "Overdue", "Paid off").forEach { filter ->
                FilterChip(
                    selected = currentFilter == filter,
                    onClick = { currentFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = GreenPrimary
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.manualSync() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (sortedProfiles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো কাস্টমার পাওয়া যায়নি", color = TextSecondary, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = sortedProfiles, key = { it.customer.id }) { profileData ->
                        CustomerRowWithBadge(profileData, overdueThreshold, onClick = { onCustomerClick(profileData.customer.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(currentTab: String, onTabSelected: (String) -> Unit, onFabClick: () -> Unit = {}) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, contentColor = GreenPrimary) {
        NavigationBarItem(
            selected = currentTab == "home",
            onClick = { onTabSelected("home") },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("ড্যাশবোর্ড") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = GreenPrimary.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = currentTab == "customer",
            onClick = { onTabSelected("customer") },
            icon = { Icon(Icons.Default.Person, contentDescription = "Customers") },
            label = { Text("কাস্টমার") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = GreenPrimary.copy(alpha = 0.1f)
            )
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = onFabClick,
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
        NavigationBarItem(
            selected = currentTab == "reports",
            onClick = { onTabSelected("reports") },
            icon = { Icon(Icons.Default.BarChart, contentDescription = "Reports") },
            label = { Text("রিপোর্ট") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = GreenPrimary.copy(alpha = 0.1f)
            )
        )
        NavigationBarItem(
            selected = currentTab == "settings",
            onClick = { onTabSelected("settings") },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("সেটিংস") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GreenPrimary,
                selectedTextColor = GreenPrimary,
                indicatorColor = GreenPrimary.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
fun TransactionItem(transaction: Transaction, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val txFormat = remember { java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()) }
            Column {
                Text(transaction.customerName, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(txFormat.format(transaction.timestamp.toDate()), fontSize = 12.sp, color = TextSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "৳${transaction.amount}",
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == "DUE") RedNegative else GreenPrimary
                )
                Text(if (transaction.type == "DUE") "Credit Sale" else "Payment", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }

}
