package com.example.profile

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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerProfileScreen(
    onBack: () -> Unit,
    viewModel: OwnerProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
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
                    OwnerProfileEditForm(
                        initialProfile = uiState.profile ?: OwnerProfile(),
                        onSave = { profile, uri ->
                            viewModel.saveProfile(profile, uri)
                        },
                        onCancel = {
                            isEditing = false
                        },
                        isSaving = uiState.isSaving
                    )
                } else {
                    uiState.profile?.let { profile ->
                        OwnerProfileView(profile = profile)
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
fun OwnerProfileView(profile: OwnerProfile) {
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val memberSince = df.format(profile.createdAt.toDate())

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
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (profile.photoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = profile.photoUrl,
                        contentDescription = "Profile Photo",
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
                            Icons.Default.Person,
                            contentDescription = "Placeholder",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = profile.fullName.ifEmpty { "Name Not Set" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Member Since: $memberSince",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
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
                Text("Personal Information", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                
                ProfileInfoRow(Icons.Default.Email, "Email", profile.email)
                ProfileInfoRow(Icons.Default.Phone, "Phone", profile.phone)
                ProfileInfoRow(Icons.Default.CalendarMonth, "Date of Birth", profile.dateOfBirth)
                ProfileInfoRow(Icons.Default.Wc, "Gender", profile.gender)
                ProfileInfoRow(Icons.Default.LocationOn, "Address", profile.address)
                ProfileInfoRow(Icons.Default.Badge, "Account ID", profile.uid)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerProfileEditForm(
    initialProfile: OwnerProfile,
    onSave: (OwnerProfile, Uri?) -> Unit,
    onCancel: () -> Unit,
    isSaving: Boolean
) {
    var fullName by remember { mutableStateOf(initialProfile.fullName) }
    var phone by remember { mutableStateOf(initialProfile.phone) }
    var address by remember { mutableStateOf(initialProfile.address) }
    var dateOfBirth by remember { mutableStateOf(initialProfile.dateOfBirth) }
    var gender by remember { mutableStateOf(initialProfile.gender) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Photo Picker
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
                    contentDescription = "New Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (initialProfile.photoUrl.isNotEmpty()) {
                AsyncImage(
                    model = initialProfile.photoUrl,
                    contentDescription = "Current Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AddAPhoto,
                    contentDescription = "Add Photo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tap to change photo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        if (showError) {
            Text(
                "Name and Phone are required",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = initialProfile.email,
            onValueChange = {},
            label = { Text("Email (Read Only)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            readOnly = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name *") },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && fullName.isBlank()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone Number *") },
            modifier = Modifier.fillMaxWidth(),
            isError = showError && phone.isBlank()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = dateOfBirth,
            onValueChange = { dateOfBirth = it },
            label = { Text("Date of Birth") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Gender Dropdown
        var expanded by remember { mutableStateOf(false) }
        val genders = listOf("Male", "Female", "Other", "Prefer not to say")
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Gender") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                genders.forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            gender = selectionOption
                            expanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Address") },
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
                    if (fullName.isBlank() || phone.isBlank()) {
                        showError = true
                    } else {
                        showError = false
                        val updatedProfile = initialProfile.copy(
                            fullName = fullName,
                            phone = phone,
                            address = address,
                            dateOfBirth = dateOfBirth,
                            gender = gender
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
