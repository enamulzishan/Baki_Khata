package com.example.invoice

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.Customer
import com.example.MainViewModel
import com.example.Transaction
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceReceiptScreen(
    viewModel: MainViewModel,
    transactionId: String,
    onBack: () -> Unit,
    shopViewModel: com.example.shop.ShopViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val transactions by viewModel.transactions.collectAsState()
    val shopState by shopViewModel.uiState.collectAsState()
    val shopProfile = shopState.profile
    val customers by viewModel.customers.collectAsState()
    
    val transaction = transactions.find { it.id == transactionId } ?: return
    val customer = customers.find { it.id == transaction.customerId } ?: return
    
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var isPrinting by remember { mutableStateOf(false) }
    
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val tf = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateStr = df.format(transaction.timestamp.toDate())
    val timeStr = tf.format(transaction.timestamp.toDate())

    LaunchedEffect(transaction) {
        val qrData = "INV:${transaction.invoiceNumber}, CUST:${customer.customerCode}, TX:${transaction.id}"
        qrBitmap = QRCodeGenerator.generateQRCode(qrData, 400, 400)
        pdfFile = PdfGenerator.generateInvoicePdf(context, transaction, customer)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Invoice Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        pdfFile?.let { file ->
                            val uri: Uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Invoice"))
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF")
                    }
                }
            )
        },
        bottomBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        isPrinting = true
                        coroutineScope.launch {
                            val payload = buildList<ByteArray> {
                                add(EscPosFormatter.INIT)
                                add(EscPosFormatter.ALIGN_CENTER)
                                add(EscPosFormatter.BOLD_ON)
                                add(EscPosFormatter.text("${if (shopProfile?.shopName.isNullOrBlank()) "SHOP NAME" else shopProfile!!.shopName}\n"))
                                add(EscPosFormatter.BOLD_OFF)
                                add(EscPosFormatter.text("${if (shopProfile?.address.isNullOrBlank()) "123 Main Street" else shopProfile!!.address}\n"))
                                add(EscPosFormatter.text("Invoice: ${transaction.invoiceNumber ?: "N/A"}\n"))
                                add(EscPosFormatter.ALIGN_LEFT)
                                add(EscPosFormatter.text("Customer: ${customer.name}\n"))
                                add(EscPosFormatter.text("Amount: ${transaction.amount}\n"))
                                add(EscPosFormatter.text("Total Due: ${customer.totalDue}\n"))
                                add(EscPosFormatter.ALIGN_CENTER)
                                add(EscPosFormatter.text("Thank you!\n\n\n"))
                                add(EscPosFormatter.CUT)
                            }
                            // val manager = BluetoothPrinterManager(context)
                            // if (manager.getPairedPrinters().isNotEmpty()) {
                            //     manager.connect(manager.getPairedPrinters().first())
                            //     payload.forEach { manager.printData(it) }
                            //     manager.disconnect()
                            // }
                            kotlinx.coroutines.delay(1000)
                            isPrinting = false
                            android.widget.Toast.makeText(context, "Receipt sent to printer", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isPrinting
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPrinting) "Printing..." else "Print Receipt")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Invoice Paper
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (shopProfile?.shopName.isNullOrBlank()) "SHOP NAME" else shopProfile!!.shopName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(if (shopProfile?.address.isNullOrBlank()) "123 Main Street, City" else shopProfile!!.address, fontSize = 12.sp, color = Color.Gray)
                    Text("Phone: ${if (shopProfile?.phone.isNullOrBlank()) "+880 1234 567890" else shopProfile!!.phone}", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Invoice No: ${transaction.invoiceNumber ?: "N/A"}", fontSize = 12.sp, color = Color.Black)
                            Text("Tx ID: ${transaction.id.take(8)}...", fontSize = 12.sp, color = Color.Black)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Date: $dateStr", fontSize = 12.sp, color = Color.Black)
                            Text("Time: $timeStr", fontSize = 12.sp, color = Color.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Bill To:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
                        Text(customer.name, fontSize = 12.sp, color = Color.Black)
                        Text("Code: ${customer.customerCode}", fontSize = 12.sp, color = Color.Black)
                        Text(customer.phone, fontSize = 12.sp, color = Color.Black)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Items Header
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Description", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(2f))
                        Text("Qty", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(0.5f))
                        Text("Price", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f))
                        Text("Subtotal", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val desc = if (transaction.type == "DUE") "Credit Sale" else "Payment"
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(desc, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(2f))
                        Text("1", fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(0.5f))
                        Text("৳${transaction.amount}", fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f))
                        Text("৳${transaction.amount}", fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Summary
                    val previousDue = if (transaction.type == "DUE") {
                        transaction.remainingDue + transaction.paidAmount - transaction.saleAmount
                    } else {
                        transaction.remainingDue + transaction.paidAmount
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Row(modifier = Modifier.fillMaxWidth(0.6f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 12.sp, color = Color.Black)
                            Text("৳${transaction.amount}", fontSize = 12.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(0.6f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount:", fontSize = 12.sp, color = Color.Black)
                            Text("৳0", fontSize = 12.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(0.6f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Prev Due:", fontSize = 12.sp, color = Color.Black)
                            Text("৳$previousDue", fontSize = 12.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(0.6f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paid:", fontSize = 12.sp, color = Color.Black)
                            Text("৳${transaction.paidAmount}", fontSize = 12.sp, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(0.6f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Remaining:", fontSize = 12.sp, color = Color.Black)
                            Text("৳${transaction.remainingDue}", fontSize = 12.sp, color = Color.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(0.6f), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Grand Total:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                            Text("৳${transaction.amount}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    qrBitmap?.let { bmp ->
                        Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR Code", modifier = Modifier.size(100.dp))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Thank you for your business!", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
