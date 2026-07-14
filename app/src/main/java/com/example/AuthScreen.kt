package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import com.example.ui.theme.*
import com.example.profile.OwnerProfile
import com.example.shop.ShopProfile
import kotlinx.coroutines.tasks.await

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isLoginMode by remember { mutableStateOf(true) }
    
    if (isLoginMode) {
        LoginView(
            onAuthSuccess = onAuthSuccess,
            onSwitchToRegister = { isLoginMode = false }
        )
    } else {
        RegistrationView(
            onAuthSuccess = onAuthSuccess,
            onSwitchToLogin = { isLoginMode = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginView(onAuthSuccess: () -> Unit, onSwitchToRegister: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val auth = FirebaseAuth.getInstance()
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    
    if (showForgotPasswordDialog) {
        var resetEmail by remember { mutableStateOf(email) }
        var resetLoading by remember { mutableStateOf(false) }
        var resetMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = { Text("Reset Password") },
            text = {
                Column {
                    Text("Enter your email address to receive a password reset link.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (resetMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(resetMessage!!, color = if (resetMessage!!.contains("sent")) GreenPrimary else RedNegative, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.isNotBlank()) {
                            resetLoading = true
                            auth.sendPasswordResetEmail(resetEmail)
                                .addOnCompleteListener { task ->
                                    resetLoading = false
                                    if (task.isSuccessful) {
                                        resetMessage = "Password reset email sent!"
                                    } else {
                                        resetMessage = task.exception?.message ?: "Failed to send reset email."
                                    }
                                }
                        }
                    },
                    enabled = !resetLoading
                ) {
                    if (resetLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Send Link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "বাকি খাতা",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = GreenPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Shopkeeper Login",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                }
            }
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showForgotPasswordDialog = true }) {
                Text("Forgot Password?", color = GreenPrimary)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isLoading) {
            CircularProgressIndicator(color = GreenPrimary)
        } else {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Please enter both email and password."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    
                    auth.signInWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                onAuthSuccess()
                            } else {
                                errorMessage = task.exception?.message ?: "Login failed."
                                isLoading = false
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = "Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage!!, color = RedNegative, fontSize = 14.sp)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("New Shopkeeper? ", color = TextSecondary)
            Text(
                "Register here", 
                color = GreenPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSwitchToRegister() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationView(onAuthSuccess: () -> Unit, onSwitchToLogin: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    
    // Owner Info
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Shop Info
    var shopName by remember { mutableStateOf("") }
    var shopAddress by remember { mutableStateOf("") }
    var businessCategory by remember { mutableStateOf("") }
    
    val categories = listOf("Grocery Store", "Pharmacy", "Clothing Store", "Electronics Store", "Hardware Store", "Restaurant", "Mobile Shop", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }
    
    // Recommended/Optional Info
    var district by remember { mutableStateOf("") }
    var upazila by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("BDT ৳") }
    var language by remember { mutableStateOf("বাংলা") }
    
    val currencies = listOf("BDT ৳", "USD $", "INR ₹")
    var currencyExpanded by remember { mutableStateOf(false) }
    
    val languages = listOf("বাংলা", "English")
    var languageExpanded by remember { mutableStateOf(false) }
    
    var whatsapp by remember { mutableStateOf("") }
    var facebook by remember { mutableStateOf("") }
    var tradeLicense by remember { mutableStateOf("") }
    var shopDescription by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // App Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenPrimary)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Shopkeeper Registration", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("👤 Owner Information (Required)", fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword, onValueChange = { confirmPassword = it }, label = { Text("Confirm Password") }, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("🏪 Shop Information (Required)", fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Shop Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = shopAddress, onValueChange = { shopAddress = it }, label = { Text("Shop Address") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = businessCategory,
                    onValueChange = { },
                    label = { Text("Business Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                businessCategory = selectionOption
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("📍 Location (Recommended)", fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = upazila, onValueChange = { upazila = it }, label = { Text("Upazila/Area") }, modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("💰 Business Settings (Recommended)", fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = !currencyExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = currency,
                        onValueChange = { },
                        label = { Text("Currency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        currencies.forEach { selectionOption ->
                            DropdownMenuItem(text = { Text(selectionOption) }, onClick = { currency = selectionOption; currencyExpanded = false })
                        }
                    }
                }
                
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = !languageExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = language,
                        onValueChange = { },
                        label = { Text("Language") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        languages.forEach { selectionOption ->
                            DropdownMenuItem(text = { Text(selectionOption) }, onClick = { language = selectionOption; languageExpanded = false })
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("🌐 Optional Fields", fontWeight = FontWeight.Bold, color = GreenPrimary, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = whatsapp, onValueChange = { whatsapp = it }, label = { Text("WhatsApp") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = facebook, onValueChange = { facebook = it }, label = { Text("Facebook") }, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = tradeLicense, onValueChange = { tradeLicense = it }, label = { Text("Trade License Number") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(value = shopDescription, onValueChange = { shopDescription = it }, label = { Text("Shop Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            
            Spacer(modifier = Modifier.height(24.dp))
            if (errorMessage != null) {
                Text(text = errorMessage!!, color = RedNegative, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (isLoading) {
                CircularProgressIndicator(color = GreenPrimary)
            } else {
                Button(
                    onClick = {
                        if (fullName.isBlank() || phone.isBlank() || email.isBlank() || password.isBlank() || shopName.isBlank() || businessCategory.isBlank()) {
                            errorMessage = "Please fill out all required fields."
                            return@Button
                        }
                        if (password != confirmPassword) {
                            errorMessage = "Passwords do not match."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid
                                    if (userId != null) {
                                        val shopId = java.util.UUID.randomUUID().toString()
                                        
                                        val ownerProfile = OwnerProfile(
                                            uid = userId,
                                            fullName = fullName,
                                            email = email,
                                            phone = phone,
                                            address = shopAddress
                                        )
                                        
                                        val shopProfile = ShopProfile(
                                            shopId = shopId,
                                            ownerUserId = userId,
                                            shopName = shopName,
                                            ownerName = fullName,
                                            phone = phone,
                                            email = email,
                                            address = shopAddress,
                                            businessCategory = businessCategory,
                                            district = district,
                                            upazila = upazila,
                                            currency = currency,
                                            language = language,
                                            whatsapp = whatsapp,
                                            facebook = facebook,
                                            tradeLicense = tradeLicense,
                                            description = shopDescription
                                        )
                                        
                                        coroutineScope.launch {
                                            try {
                                                db.collection("users").document(userId).set(ownerProfile).await()
                                                db.collection("shops").document(shopId).set(shopProfile).await()
                                                onAuthSuccess()
                                            } catch (e: Exception) {
                                                errorMessage = "Error saving profile: ${e.message}"
                                                isLoading = false
                                            }
                                        }
                                    }
                                } else {
                                    errorMessage = task.exception?.message ?: "Registration failed."
                                    isLoading = false
                                }
                            }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(text = "Register", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text("Already have an account? ", color = TextSecondary)
                Text(
                    "Login here", 
                    color = GreenPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onSwitchToLogin() }
                )
            }
        }
    }
}
