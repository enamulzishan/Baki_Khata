package com.example.shop

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopProfileScreen(
    onBack: () -> Unit,
    viewModel: ShopViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    // If profile is null and not loading, we should go into edit mode to create one
    LaunchedEffect(uiState.isLoading, uiState.profile) {
        if (!uiState.isLoading && uiState.profile == null && !isEditing) {
            isEditing = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shop Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isEditing && uiState.profile != null) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GreenPrimary
                )
            } else if (uiState.error != null && !isEditing) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadProfile() }) {
                        Text("Retry")
                    }
                }
            } else {
                if (isEditing) {
                    ShopProfileEditForm(
                        initialProfile = uiState.profile ?: ShopProfile(),
                        onSave = { profile, uri ->
                            viewModel.saveProfile(profile, uri)
                        },
                        onCancel = {
                            if (uiState.profile != null) {
                                isEditing = false
                            } else {
                                onBack()
                            }
                        },
                        isSaving = uiState.isSaving
                    )
                } else {
                    uiState.profile?.let { profile ->
                        ShopProfileView(profile = profile)
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            isEditing = false
            viewModel.resetSuccess()
        }
    }
}

@Composable
fun ShopProfileView(profile: ShopProfile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (profile.logoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profile.logoUrl,
                        contentDescription = "Shop Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Placeholder",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = profile.shopName.ifEmpty { "Unnamed Shop" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (profile.businessCategory.isNotEmpty()) {
                    Text(
                        text = profile.businessCategory,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Contact Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
                ProfileInfoRow(Icons.Default.Person, "Owner", profile.ownerName)
                ProfileInfoRow(Icons.Default.Phone, "Phone", profile.phone)
                if (profile.email.isNotEmpty()) ProfileInfoRow(Icons.Default.Email, "Email", profile.email)
                if (profile.whatsapp.isNotEmpty()) ProfileInfoRow(Icons.Default.Message, "WhatsApp", profile.whatsapp)
                ProfileInfoRow(Icons.Default.LocationOn, "Address", profile.address)
                if (profile.website.isNotEmpty()) ProfileInfoRow(Icons.Default.Language, "Website", profile.website)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Business Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
                if (profile.tradeLicense.isNotEmpty()) ProfileInfoRow(Icons.Default.Assignment, "Trade License", profile.tradeLicense)
                if (profile.taxId.isNotEmpty()) ProfileInfoRow(Icons.Default.Receipt, "Tax ID", profile.taxId)
                ProfileInfoRow(Icons.Default.AttachMoney, "Currency", profile.currency)
                if (profile.facebook.isNotEmpty()) ProfileInfoRow(Icons.Default.ThumbUp, "Facebook", profile.facebook)
                if (profile.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Description", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.description, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.ifEmpty { "Not Provided" }, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ShopProfileEditForm(
    initialProfile: ShopProfile,
    onSave: (ShopProfile, Uri?) -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean
) {
    var shopName by remember { mutableStateOf(initialProfile.shopName) }
    var ownerName by remember { mutableStateOf(initialProfile.ownerName) }
    var phone by remember { mutableStateOf(initialProfile.phone) }
    var address by remember { mutableStateOf(initialProfile.address) }
    var email by remember { mutableStateOf(initialProfile.email) }
    var website by remember { mutableStateOf(initialProfile.website) }
    var businessCategory by remember { mutableStateOf(initialProfile.businessCategory) }
    var tradeLicense by remember { mutableStateOf(initialProfile.tradeLicense) }
    var taxId by remember { mutableStateOf(initialProfile.taxId) }
    var currency by remember { mutableStateOf(initialProfile.currency) }
    var facebook by remember { mutableStateOf(initialProfile.facebook) }
    var whatsapp by remember { mutableStateOf(initialProfile.whatsapp) }
    var description by remember { mutableStateOf(initialProfile.description) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Validation state
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Picker
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { galleryLauncher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "New Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (initialProfile.logoUrl.isNotEmpty()) {
                AsyncImage(
                    model = initialProfile.logoUrl,
                    contentDescription = "Current Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = "Add Logo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tap to change logo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        if (showError) {
            Text(
                "Please fill in all required fields (Shop Name, Owner, Phone, Address)",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = shopName,
            onValueChange = { shopName = it },
            label = { Text("Shop Name *") },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && shopName.isBlank()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = ownerName,
            onValueChange = { ownerName = it },
            label = { Text("Owner Name *") },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && ownerName.isBlank()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Mobile Number *") },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && phone.isBlank()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Shop Address *") },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && address.isBlank()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = businessCategory,
            onValueChange = { businessCategory = it },
            label = { Text("Business Category") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = website,
            onValueChange = { website = it },
            label = { Text("Website") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it },
            label = { Text("Currency") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = tradeLicense,
            onValueChange = { tradeLicense = it },
            label = { Text("Trade License Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = taxId,
            onValueChange = { taxId = it },
            label = { Text("Tax ID") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = whatsapp,
            onValueChange = { whatsapp = it },
            label = { Text("WhatsApp Number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = facebook,
            onValueChange = { facebook = it },
            label = { Text("Facebook Page") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (shopName.isBlank() || ownerName.isBlank() || phone.isBlank() || address.isBlank()) {
                        showError = true
                    } else {
                        showError = false
                        val updatedProfile = initialProfile.copy(
                            shopName = shopName,
                            ownerName = ownerName,
                            phone = phone,
                            address = address,
                            email = email,
                            website = website,
                            businessCategory = businessCategory,
                            tradeLicense = tradeLicense,
                            taxId = taxId,
                            currency = currency,
                            whatsapp = whatsapp,
                            facebook = facebook,
                            description = description
                        )
                        onSave(updatedProfile, selectedImageUri)
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Save")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
