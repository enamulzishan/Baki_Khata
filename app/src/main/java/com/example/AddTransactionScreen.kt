package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onTransactionSaved: (String) -> Unit,
    preselectedType: String = "Credit Sale"
) {
    val coroutineScope = rememberCoroutineScope()
    var transactionType by remember { mutableStateOf(preselectedType) } // "Credit Sale" or "Payment Collection"
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    

    var saleAmount by remember { mutableStateOf("") }
    var paidAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var showPaymentDropdown by remember { mutableStateOf(false) }
    
    var notes by remember { mutableStateOf("") }
    
    val calendar = Calendar.getInstance()
    var transactionDate by remember { mutableStateOf(calendar.timeInMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = transactionDate)
    
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var receiptImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRiskWarningDialog by remember { mutableStateOf(false) }
    
    // Check risk level for selected customer
    val profile = selectedCustomer?.let { viewModel.getCustomerProfile(it.id)?.profile }

    
    val customers by viewModel.customers.collectAsState()
    val filteredCustomers = customers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.phone.contains(searchQuery, ignoreCase = true) ||
        it.customerCode.contains(searchQuery, ignoreCase = true)
    }
    val context = LocalContext.current
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        receiptImageUri = uri
    }

    val parsedSale = saleAmount.toLongOrNull() ?: 0L
    val parsedPaid = paidAmount.toLongOrNull() ?: 0L
    val remainingDue = if (transactionType == "Credit Sale") {
        maxOf(0L, parsedSale - parsedPaid)
    } else {
        0L
    }

    val performSave = {
        isUploading = true
        coroutineScope.launch {
            val transactionId = viewModel.saveTransaction(
                customer = selectedCustomer!!,
                type = transactionType,
                saleAmount = parsedSale,
                paidAmount = parsedPaid,
                remainingDue = remainingDue,
                paymentMethod = paymentMethod,
                notes = notes,
                imageUri = receiptImageUri,
                transactionDate = transactionDate
            )
            isUploading = false
            if (transactionId != null) {
                android.widget.Toast.makeText(context, "Transaction saved successfully.", android.widget.Toast.LENGTH_SHORT).show()
                onTransactionSaved(transactionId)
            } else {
                errorMessage = "Failed to save transaction."
            }
        }
    }

    if (showRiskWarningDialog) {
        AlertDialog(
            onDismissRequest = { showRiskWarningDialog = false },
            title = { Text("High Credit Risk") },
            text = { Text("This customer has a high credit risk (Score: ${profile?.score}). Are you sure you want to continue with this credit sale?") },
            confirmButton = {
                Button(onClick = {
                    showRiskWarningDialog = false
                    performSave()
                }, colors = ButtonDefaults.buttonColors(containerColor = RedNegative)) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRiskWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction", fontWeight = FontWeight.Bold, color = GreenPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = GreenPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = RedNegative, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isUploading
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (selectedCustomer == null) {
                                errorMessage = "Customer required."
                                return@Button
                            }
                            if (transactionType == "Credit Sale" && parsedSale <= 0) {
                                errorMessage = "Sale Amount > 0 required for Credit Sale."
                                return@Button
                            }
                            if (parsedPaid < 0) {
                                errorMessage = "Paid Amount >= 0 required."
                                return@Button
                            }
                            if (transactionType == "Credit Sale" && parsedPaid > parsedSale) {
                                errorMessage = "Paid Amount cannot exceed Sale Amount."
                                return@Button
                            }
                            if (transactionType == "Payment Collection" && parsedPaid <= 0) {
                                errorMessage = "Paid Amount must be > 0 for Payment Collection."
                                return@Button
                            }
                            errorMessage = null
                            
                            if (transactionType == "Credit Sale" && profile != null && profile.score < 40) {
                                showRiskWarningDialog = true
                                return@Button
                            }
                            
                            performSave()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Save", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Customer Selection
            ExposedDropdownMenuBox(
                expanded = showCustomerDropdown,
                onExpandedChange = { showCustomerDropdown = !showCustomerDropdown }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        showCustomerDropdown = true
                        selectedCustomer = null
                    },
                    label = { Text("Search Customer *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Type name, phone, or code") }
                )
                ExposedDropdownMenu(
                    expanded = showCustomerDropdown,
                    onDismissRequest = { showCustomerDropdown = false }
                ) {
                    filteredCustomers.forEach { cust ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.Bold)
                                    Text("📞 ${cust.phone} | 🆔 ${cust.customerCode}", fontSize = 12.sp, color = TextSecondary)
                                }
                            },
                            onClick = {
                                selectedCustomer = cust
                                searchQuery = "${cust.name} (${cust.customerCode})"
                                showCustomerDropdown = false
                            }
                        )
                    }
                }
            }

            // Transaction Type
            Text("Transaction Type", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = transactionType == "Credit Sale",
                        onClick = { transactionType = "Credit Sale" },
                        colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                    )
                    Text("Credit Sale")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = transactionType == "Payment Collection",
                        onClick = { transactionType = "Payment Collection" },
                        colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary)
                    )
                    Text("Payment Collection")
                }
            }

            // Amounts
            if (transactionType == "Credit Sale") {
                OutlinedTextField(
                    value = saleAmount,
                    onValueChange = { saleAmount = it },
                    label = { Text("Sale Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            OutlinedTextField(
                value = paidAmount,
                onValueChange = { paidAmount = it },
                label = { Text(if (transactionType == "Credit Sale") "Paid Amount" else "Paid Amount *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (transactionType == "Credit Sale") {
                OutlinedTextField(
                    value = remainingDue.toString(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Remaining Due") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = TextPrimary,
                        disabledBorderColor = CardBorder,
                        disabledLabelColor = TextSecondary
                    ),
                    enabled = false
                )
            }

            // Payment Method
            ExposedDropdownMenuBox(
                expanded = showPaymentDropdown,
                onExpandedChange = { showPaymentDropdown = !showPaymentDropdown }
            ) {
                OutlinedTextField(
                    value = paymentMethod,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Method") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                )
                val methods = listOf("Cash", "Bkash", "Nagad", "Rocket", "Bank", "Other")
                ExposedDropdownMenu(
                    expanded = showPaymentDropdown,
                    onDismissRequest = { showPaymentDropdown = false }
                ) {
                    methods.forEach { m ->
                        DropdownMenuItem(
                            text = { Text(m) },
                            onClick = {
                                paymentMethod = m
                                showPaymentDropdown = false
                            }
                        )
                    }
                }
            }

            // Transaction Date
            OutlinedTextField(
                value = dateFormat.format(Date(transactionDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Transaction Date") },
                modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                enabled = false,
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = TextPrimary,
                    disabledBorderColor = GreenPrimary,
                    disabledLabelColor = GreenPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = { 
                            showDatePicker = false
                            dateState.selectedDateMillis?.let { transactionDate = it }
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DatePicker(state = dateState)
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            // Image Picker
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (receiptImageUri != null) {
                    AsyncImage(
                        model = receiptImageUri,
                        contentDescription = "Receipt",
                        modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap to change receipt", color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                } else {
                    Icon(Icons.Default.Image, contentDescription = "Add Receipt", tint = TextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Attach Receipt Image (Optional)", color = TextSecondary)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
