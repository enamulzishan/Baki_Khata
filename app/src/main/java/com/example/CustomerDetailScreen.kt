package com.example

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: MainViewModel,
    customerId: String,
    onBack: () -> Unit,
    onInvoiceClick: (String) -> Unit = {}
) {
    val customers by viewModel.customers.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    
    val customerProfile = viewModel.getCustomerProfile(customerId) ?: return
    val customer = customerProfile.customer
    val profile = customerProfile.profile
    val customerTransactions = transactions.filter { it.customerId == customerId }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showPaymentSheet by remember { mutableStateOf(false) }
    var showAddDueSheet by remember { mutableStateOf(false) }
    var deleteTxId by remember { mutableStateOf<String?>(null) }
    var currentTab by remember { mutableStateOf("invoices") }
    
    val format = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(customer.name, fontWeight = FontWeight.Bold, color = GreenPrimary)
                        Text(customer.customerCode, fontSize = 14.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showAddDueSheet = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedNegative),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("বাকি যোগ করুন\n(Add Due)", fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Button(
                    onClick = { showPaymentSheet = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("কালেকশন করুন\n(Collect)", fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Customer Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("মোবাইল: ${customer.phone}", color = TextSecondary)
                    if (customer.address.isNotEmpty()) {
                        Text("ঠিকানা: ${customer.address}", color = TextSecondary)
                    }
                    if (customer.creditLimit > 0) {
                        Text("লিমিট: ৳${customer.creditLimit}", color = TextSecondary)
                    }
                    val sinceStr = format.format(customer.createdAt.toDate())
                    Text("কাস্টমার যুক্ত হয়েছে: $sinceStr", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("মোট বাকি", color = TextSecondary, fontSize = 12.sp)
                            Text("৳${customer.totalDue}", color = RedNegative, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("মোট জমা", color = TextSecondary, fontSize = 12.sp)
                            Text("৳${customer.totalPaid}", color = GreenPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Call, contentDescription = "Call", tint = GreenPrimary)
                        }
                        IconButton(onClick = {
                            val msg = "Hello ${customer.name},\nThis is a reminder that your outstanding balance is ৳${customer.totalDue}.\nPlease make the payment at your earliest convenience.\nThank you."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:${customer.phone}"))
                            intent.putExtra("sms_body", msg)
                            context.startActivity(intent)
                        }) {
                            Icon(Icons.Default.Message, contentDescription = "SMS", tint = GreenPrimary)
                        }
                        IconButton(onClick = {
                            val msg = "Hello ${customer.name},\nThis is a reminder that your outstanding balance is ৳${customer.totalDue}.\nPlease make the payment at your earliest convenience.\nThank you."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=${customer.phone}&text=${Uri.encode(msg)}"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // WhatsApp not installed
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = GreenPrimary) // Using Share icon as placeholder for WhatsApp
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TabRow(selectedTabIndex = if(currentTab == "overview") 0 else 1, containerColor = BackgroundColor, contentColor = GreenPrimary) {
                Tab(selected = currentTab == "overview", onClick = { currentTab = "overview" }, text = { Text("Overview") })
                Tab(selected = currentTab == "invoices", onClick = { currentTab = "invoices" }, text = { Text("Invoices") })
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (currentTab == "overview") {
                // Overview Content
                Text("Customer Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Transactions: ${customerTransactions.size}", color = TextSecondary)
                Text("Total Due: ৳${customer.totalDue}", color = TextSecondary)
                Text("Total Paid: ৳${customer.totalPaid}", color = TextSecondary)
            } else {
            
            if (customerTransactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("কোনো লেনদেন নেই", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = customerTransactions, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onInvoiceClick(tx.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val dateStr = format.format(tx.timestamp.toDate())
                                    Text(dateStr, color = TextSecondary, fontSize = 12.sp)
                                    Row {
                                        IconButton(onClick = { /* Placeholder */ }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GreenPrimary, modifier = Modifier.size(20.dp))
                                        }
                                        IconButton(onClick = { deleteTxId = tx.id }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RedNegative, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(if (tx.type == "DUE") "বাকি (Due)" else "জমা (Deposit)", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        if (tx.paymentMethod.isNotEmpty()) {
                                            Text(tx.paymentMethod, fontSize = 12.sp, color = TextSecondary)
                                        }
                                        if (tx.notes.isNotEmpty()) {
                                            Text(tx.notes, fontSize = 12.sp, color = TextSecondary)
                                        }
                                    }
                                    Text(
                                        "৳${tx.amount}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = if (tx.type == "DUE") RedNegative else GreenPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
    
    if (deleteTxId != null) {
        AlertDialog(
            onDismissRequest = { deleteTxId = null },
            title = { Text("লেনদেন মুছুন", color = RedNegative) },
            text = { Text("আপনি কি নিশ্চিত যে আপনি এই লেনদেনটি মুছে ফেলতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(deleteTxId!!, customerId)
                        deleteTxId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedNegative)
                ) {
                    Text("মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTxId = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            containerColor = Color.White
        ) {
            PaymentSheetContent(
                customer = customer,
                onDismiss = { showPaymentSheet = false },
                onPay = { amount, method, notes ->
                    viewModel.collectPayment(customer.id, amount, method, notes)
                    showPaymentSheet = false
                }
            )
        }
    }

    if (showAddDueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddDueSheet = false },
            containerColor = Color.White
        ) {
            AddDueSheetContent(
                customer = customer,
                onDismiss = { showAddDueSheet = false },
                onAddDue = { amount, notes ->
                    viewModel.addDue(customer, amount, notes)
                    showAddDueSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentSheetContent(
    customer: Customer,
    onDismiss: () -> Unit,
    onPay: (Long, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("নগদ (Cash)") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    val methods = listOf("নগদ (Cash)", "বিকাশ (bKash)", "নগদ (Nagad)", "রকেট (Rocket)", "ব্যাংক (Bank)")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("টাকা সংগ্রহ করুন", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
        Text("বর্তমান বাকি: ৳${customer.totalDue}", fontSize = 16.sp, color = RedNegative, fontWeight = FontWeight.SemiBold)
        
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("টাকার পরিমাণ *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp)
        )
        
        Text("পেমেন্ট মাধ্যম", fontSize = 14.sp, color = TextSecondary)
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(methods) { m ->
                FilterChip(
                    selected = method == m,
                    onClick = { method = m },
                    label = { Text(m) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GreenPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = GreenPrimary
                    )
                )
            }
        }
        
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("নোট (ঐচ্ছিক)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary, focusedLabelColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (error != null) {
            Text(error!!, color = RedNegative, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val amt = amount.toLongOrNull() ?: 0L
                if (amt <= 0) {
                    error = "সঠিক টাকার পরিমাণ দিন"
                } else if (amt > customer.totalDue) {
                    error = "বাকি টাকার চেয়ে বেশি সংগ্রহ করা যাবে না"
                } else {
                    onPay(amt, method, notes)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("সংরক্ষণ করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDueSheetContent(
    customer: Customer,
    onDismiss: () -> Unit,
    onAddDue: (Long, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("বাকি যোগ করুন (Add Due)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = RedNegative)
        Text("বর্তমান বাকি: ৳${customer.totalDue}", fontSize = 16.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
        
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("টাকার পরিমাণ *") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedNegative, focusedLabelColor = RedNegative),
            shape = RoundedCornerShape(12.dp)
        )
        
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("নোট (ঐচ্ছিক)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = RedNegative, focusedLabelColor = RedNegative),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (error != null) {
            Text(error!!, color = RedNegative, fontSize = 12.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val amt = amount.toLongOrNull() ?: 0L
                if (amt <= 0) {
                    error = "সঠিক টাকার পরিমাণ দিন"
                } else {
                    onAddDue(amt, notes)
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RedNegative),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("যোগ করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
