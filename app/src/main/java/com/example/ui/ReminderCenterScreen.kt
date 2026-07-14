package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.Customer
import com.example.settings.SettingsViewModel
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedNegative
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderCenterScreen(
    onBack: () -> Unit,
    viewModel: ReminderViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showTemplateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.selectedCustomerIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Text("All", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Text("Clear", fontSize = 14.sp)
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.selectedCustomerIds.isNotEmpty()) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${uiState.selectedCustomerIds.size} Selected", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showTemplateDialog = true }) {
                                Text("Send SMS")
                            }
                            Button(
                                onClick = { showTemplateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Text("WhatsApp")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().background(Color(0xFFF8F9FA))) {
            // Filters
            ScrollableTabRow(
                selectedTabIndex = listOf("All", "Due Today", "1-7 Days", "8-15 Days", "16-30 Days", "30+ Days").indexOf(uiState.filterType).coerceAtLeast(0),
                edgePadding = 16.dp
            ) {
                listOf("All", "Due Today", "1-7 Days", "8-15 Days", "16-30 Days", "30+ Days").forEach { type ->
                    Tab(
                        selected = uiState.filterType == type,
                        onClick = { viewModel.setFilterType(type) },
                        text = { Text(type) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredCustomers, key = { it.id }) { customer ->
                    val isSelected = uiState.selectedCustomerIds.contains(customer.id)
                    val daysOverdue = (System.currentTimeMillis() - customer.lastTransactionDate.toDate().time) / (24 * 60 * 60 * 1000L)
                    val lastReminder = uiState.reminderHistory.filter { it.customerId == customer.id }.maxByOrNull { it.timestamp }
                    
                    CustomerReminderCard(
                        customer = customer,
                        isSelected = isSelected,
                        daysOverdue = daysOverdue,
                        lastReminderDate = lastReminder?.timestamp?.toDate()?.toString() ?: "Never",
                        onClick = { viewModel.toggleSelection(customer.id) },
                        onSendSms = {
                            val msg = viewModel.getProcessedMessage(customer, settingsState.shopName)
                            sendSms(context, customer.phone, msg)
                            viewModel.recordReminder(customer.id, customer.name, "SMS", msg)
                        },
                        onSendWhatsapp = {
                            val msg = viewModel.getProcessedMessage(customer, settingsState.shopName)
                            sendWhatsApp(context, customer.phone, msg)
                            viewModel.recordReminder(customer.id, customer.name, "WHATSAPP", msg)
                        }
                    )
                }
            }
        }
        
        if (showTemplateDialog) {
            AlertDialog(
                onDismissRequest = { showTemplateDialog = false },
                title = { Text("Select Message Template") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.templates.forEachIndexed { index, template ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setSelectedTemplate(index) }
                                    .background(if (uiState.selectedTemplateIndex == index) GreenPrimary.copy(alpha=0.1f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.selectedTemplateIndex == index,
                                    onClick = { viewModel.setSelectedTemplate(index) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(template, fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        showTemplateDialog = false
                        val shopName = settingsState.shopName
                        // Process selected customers
                        val customersToMessage = uiState.customers.filter { uiState.selectedCustomerIds.contains(it.id) }
                        if (customersToMessage.size == 1) {
                            // Single send - use explicit intent for SMS or WhatsApp
                            val customer = customersToMessage.first()
                            val msg = viewModel.getProcessedMessage(customer, shopName)
                            // Here we could open the chooser for one, but the UI has "Send SMS" and "WhatsApp" buttons separately.
                            // However, we merged it into a single bulk send. Let's just default to SMS for bulk.
                        }
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTemplateDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun CustomerReminderCard(
    customer: Customer,
    isSelected: Boolean,
    daysOverdue: Long,
    lastReminderDate: String,
    onClick: () -> Unit,
    onSendSms: () -> Unit,
    onSendWhatsapp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) GreenPrimary.copy(alpha = 0.1f) else Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) GreenPrimary else Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${customer.phone} | ${customer.customerCode}", fontSize = 12.sp, color = TextSecondary)
                    }
                }
                Text("৳${customer.totalDue}", fontWeight = FontWeight.Bold, color = RedNegative)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Overdue: $daysOverdue days", fontSize = 12.sp, color = RedNegative)
                Text("Last Reminder: $lastReminderDate", fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSendSms, modifier = Modifier.weight(1f).height(40.dp)) {
                    Text("SMS", fontSize = 12.sp)
                }
                Button(
                    onClick = onSendWhatsapp,
                    modifier = Modifier.weight(1f).height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("WhatsApp", fontSize = 12.sp)
                }
            }
        }
    }
}

fun sendSms(context: Context, phone: String, message: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone"))
        intent.putExtra("sms_body", message)
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun sendWhatsApp(context: Context, phone: String, message: String) {
    try {
        var formattedPhone = phone
        if (!formattedPhone.startsWith("+")) {
            // Assuming BD code
            if (formattedPhone.startsWith("0")) {
                formattedPhone = "+88$formattedPhone"
            } else {
                formattedPhone = "+880$formattedPhone"
            }
        }
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
