package com.example

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.GreenPrimary
import com.example.ui.theme.RedNegative
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    correctPin: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val biometricManager = remember { BiometricManager.from(context) }
    val canAuthenticate = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    LaunchedEffect(Unit) {
        if (canAuthenticate && context is FragmentActivity) {
            showBiometricPrompt(context, onUnlocked) {
                errorMessage = "Biometric authentication failed. Please use PIN."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = "Lock Icon", tint = GreenPrimary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("App Locked", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter 4-digit PIN to unlock", fontSize = 16.sp, color = TextSecondary)
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = enteredPin,
            onValueChange = { 
                if (it.length <= 4) enteredPin = it
                if (enteredPin == correctPin && correctPin.isNotEmpty()) {
                    onUnlocked()
                } else if (enteredPin.length == 4 && enteredPin != correctPin) {
                    errorMessage = "Incorrect PIN"
                    enteredPin = ""
                }
            },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMessage!!, color = RedNegative, fontSize = 14.sp)
        }
        
        if (canAuthenticate) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { 
                    if (context is FragmentActivity) {
                        showBiometricPrompt(context, onUnlocked) {
                            errorMessage = "Biometric authentication failed."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = GreenPrimary),
                elevation = null
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint", modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Fingerprint")
            }
        }
    }
}

private fun showBiometricPrompt(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError()
            }
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError()
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock App")
        .setSubtitle("Use your fingerprint to unlock")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()

    biometricPrompt.authenticate(promptInfo)
}
