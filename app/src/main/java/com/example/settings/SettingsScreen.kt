package com.example.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onShopProfileClick: () -> Unit = {},
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showShopInfoDialog by remember { mutableStateOf(false) }
    var showAppearanceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTimezoneDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = GreenPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        containerColor = BackgroundColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionTitle("General")
                SettingsItem(Icons.Default.Store, "Shop Information", "Name, Logo, Address") { onShopProfileClick() }
                SettingsItem(Icons.Default.Palette, "Appearance", uiState.themeMode) { showAppearanceDialog = true }
                SettingsItem(Icons.Default.Language, "Language", uiState.language) { showLanguageDialog = true }
                SettingsItem(Icons.Default.Schedule, "Timezone", uiState.timezone) { showTimezoneDialog = true }
                SettingsItem(Icons.Default.DateRange, "Date Format", uiState.dateFormat) { showDateFormatDialog = true }
            }

            item {
                SectionTitle("Security & Data")
                SettingsItem(Icons.Default.Lock, "Security", "App Lock, PIN, Biometric") { showSecurityDialog = true }
                SettingsItem(Icons.Default.CloudSync, "Backup & Restore", "Last backup: Never") { showBackupDialog = true }
                SettingsItem(Icons.Default.Sync, "Sync Data", "Sync offline changes to server") { viewModel.syncNow() }
                SettingsItem(Icons.Default.Notifications, "Notifications", "Reminders, Alerts") { showNotificationsDialog = true }
            }

            item {
                SectionTitle("Data Management")
                SettingsItem(Icons.Default.PictureAsPdf, "Export as PDF", "Download reports") { /* Export PDF */ }
                SettingsItem(Icons.Default.TableChart, "Export as Excel", "Download CSV") { /* Export Excel */ }
                SettingsItem(Icons.Default.DeleteForever, "Delete All Data", "Clear all transactions", isDestructive = true) { showDeleteDataDialog = true }
                SettingsItem(Icons.Default.PersonRemove, "Delete Account", "Permanently delete account", isDestructive = true) { showDeleteAccountDialog = true }
            }

            item {
                SectionTitle("About")
                SettingsItem(Icons.Default.Info, "App Version", "1.0.0") {}
                SettingsItem(Icons.Default.PrivacyTip, "Privacy Policy", "") {}
                SettingsItem(Icons.Default.Description, "Terms & Conditions", "") {}
                SettingsItem(Icons.Default.SupportAgent, "Contact Support", "") {}
                SettingsItem(Icons.Default.StarRate, "Rate App", "") {}
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedNegative),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout")
                    Spacer(Modifier.width(8.dp))
                    Text("Logout", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showShopInfoDialog) {
        ShopInfoDialog(
            uiState = uiState,
            onDismiss = { showShopInfoDialog = false },
            onSave = { name, owner, phone, address ->
                viewModel.saveShopInfo(name, owner, phone, address)
                showShopInfoDialog = false
            }
        )
    }

    if (showAppearanceDialog) {
        SelectionDialog("Appearance", listOf("Light Mode", "Dark Mode", "System Default"), uiState.themeMode, { showAppearanceDialog = false }) {
            viewModel.saveThemeMode(it)
            showAppearanceDialog = false
        }
    }

    if (showLanguageDialog) {
        SelectionDialog("Language", listOf("English", "বাংলা"), uiState.language, { showLanguageDialog = false }) {
            viewModel.saveLanguage(it)
            showLanguageDialog = false
        }
    }
    if (showTimezoneDialog) {
        SelectionDialog("Timezone", listOf("System Default", "UTC", "Asia/Dhaka", "America/New_York"), uiState.timezone, { showTimezoneDialog = false }) {
            viewModel.saveTimezone(it)
            showTimezoneDialog = false
        }
    }
    if (showDateFormatDialog) {
        SelectionDialog("Date Format", listOf("dd MMM yyyy", "MM/dd/yyyy", "yyyy-MM-dd"), uiState.dateFormat, { showDateFormatDialog = false }) {
            viewModel.saveDateFormat(it)
            showDateFormatDialog = false
        }
    }

    if (showSecurityDialog) {
        SecurityDialog(
            uiState = uiState,
            onDismiss = { showSecurityDialog = false },
            onSave = { enabled, pin, requireAuth ->
                viewModel.saveSecuritySettings(enabled, pin, requireAuth)
                showSecurityDialog = false
            }
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Backup & Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Last Backup: Never", color = TextSecondary)
                    Text("Status: Pending", color = TextSecondary)
                    
                    Button(onClick = { viewModel.backupData { showBackupDialog = false } }, modifier = Modifier.fillMaxWidth()) {
                        Text("Backup Now")
                    }
                    OutlinedButton(onClick = { viewModel.restoreData { showBackupDialog = false } }, modifier = Modifier.fillMaxWidth()) {
                        Text("Restore Data")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) { Text("Close") }
            }
        )
    }

    if (showNotificationsDialog) {
        NotificationsDialog(
            uiState = uiState,
            onDismiss = { showNotificationsDialog = false },
            onSave = { payment, overdue, daily, weekly, backup ->
                viewModel.saveNotificationSettings(payment, overdue, daily, weekly, backup)
                showNotificationsDialog = false
            }
        )
    }
    
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = { Text("Delete All Data") },
            text = { Text("Are you sure? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllData { showDeleteDataDialog = false } }) { Text("Delete", color = RedNegative) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    if (showDeleteAccountDialog) {
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account") },
            text = { 
                Column {
                    Text("Enter your password to confirm.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAccount(password) { 
                    showDeleteAccountDialog = false
                    if (it) onLogout()
                } }) { Text("Delete", color = RedNegative) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) { Text("Cancel") }
            }
        )
    }


    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    viewModel.logout()
                    onLogout()
                }) { Text("Logout", color = RedNegative) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = GreenPrimary,
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Setting Icon",
                tint = if (isDestructive) RedNegative else TextPrimary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDestructive) RedNegative else TextPrimary
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Setting Icon",
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun SelectionDialog(title: String, options: List<String>, selected: String, onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option == selected, onClick = { onSelect(option) })
                        Spacer(Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ShopInfoDialog(uiState: SettingsUiState, onDismiss: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf(uiState.shopName) }
    var owner by remember { mutableStateOf(uiState.ownerName) }
    var phone by remember { mutableStateOf(uiState.phoneNumber) }
    var address by remember { mutableStateOf(uiState.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Shop Information") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Shop Name") }, singleLine = true)
                OutlinedTextField(value = owner, onValueChange = { owner = it }, label = { Text("Owner Name") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, owner, phone, address) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SecurityDialog(uiState: SettingsUiState, onDismiss: () -> Unit, onSave: (Boolean, String, Boolean) -> Unit) {
    var enabled by remember { mutableStateOf(uiState.appLockEnabled) }
    var pin by remember { mutableStateOf(uiState.appPin) }
    var requireAuth by remember { mutableStateOf(uiState.requireAuth) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Security Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Enable App Lock")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                if (enabled) {
                    OutlinedTextField(
                        value = pin, 
                        onValueChange = { if (it.length <= 4) pin = it }, 
                        label = { Text("4-digit PIN") }, 
                        singleLine = true
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Require Auth on Launch")
                        Switch(checked = requireAuth, onCheckedChange = { requireAuth = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(enabled, pin, requireAuth) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NotificationsDialog(uiState: SettingsUiState, onDismiss: () -> Unit, onSave: (Boolean, Boolean, Boolean, Boolean, Boolean) -> Unit) {
    var payment by remember { mutableStateOf(uiState.notifyPayment) }
    var overdue by remember { mutableStateOf(uiState.notifyOverdue) }
    var daily by remember { mutableStateOf(uiState.notifyDaily) }
    var weekly by remember { mutableStateOf(uiState.notifyWeekly) }
    var backup by remember { mutableStateOf(uiState.notifyBackup) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notifications") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NotificationToggle("Payment Reminders", payment) { payment = it }
                NotificationToggle("Overdue Alerts", overdue) { overdue = it }
                NotificationToggle("Daily Summary", daily) { daily = it }
                NotificationToggle("Weekly Summary", weekly) { weekly = it }
                NotificationToggle("Backup Reminder", backup) { backup = it }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(payment, overdue, daily, weekly, backup) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun NotificationToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
