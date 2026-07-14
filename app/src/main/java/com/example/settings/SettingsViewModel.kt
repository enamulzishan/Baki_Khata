package com.example.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.sync.SyncManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val shopName: String = "",
    val ownerName: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val themeMode: String = "System Default",
    val language: String = "English",
    val timezone: String = "System Default",
    val dateFormat: String = "dd MMM yyyy",
    val appLockEnabled: Boolean = false,
    val appPin: String = "",
    val requireAuth: Boolean = false,
    val notifyPayment: Boolean = true,
    val notifyOverdue: Boolean = true,
    val notifyDaily: Boolean = false,
    val notifyWeekly: Boolean = false,
    val notifyBackup: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    val uiState: StateFlow<SettingsUiState> = combine(
        settingsManager.shopNameFlow,
        settingsManager.ownerNameFlow,
        settingsManager.phoneNumberFlow,
        settingsManager.addressFlow,
        settingsManager.themeModeFlow,
        settingsManager.languageFlow,
        settingsManager.timezoneFlow,
        settingsManager.dateFormatFlow,
        settingsManager.appLockEnabledFlow,
        settingsManager.appPinFlow,
        settingsManager.requireAuthFlow,
        settingsManager.notifyPaymentFlow,
        settingsManager.notifyOverdueFlow,
        settingsManager.notifyDailyFlow,
        settingsManager.notifyWeeklyFlow,
        settingsManager.notifyBackupFlow
    ) { values ->
        SettingsUiState(
            shopName = values[0] as String,
            ownerName = values[1] as String,
            phoneNumber = values[2] as String,
            address = values[3] as String,
            themeMode = values[4] as String,
            language = values[5] as String,
            timezone = values[6] as String,
            dateFormat = values[7] as String,
            appLockEnabled = values[8] as Boolean,
            appPin = values[9] as String,
            requireAuth = values[10] as Boolean,
            notifyPayment = values[11] as Boolean,
            notifyOverdue = values[12] as Boolean,
            notifyDaily = values[13] as Boolean,
            notifyWeekly = values[14] as Boolean,
            notifyBackup = values[15] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun syncNow() {
        SyncManager(getApplication()).enqueueSync()
    }

    fun saveShopInfo(name: String, owner: String, phone: String, address: String) {
        viewModelScope.launch {
            settingsManager.saveShopInfo(name, owner, phone, address)
            // Save to Firestore
            val uid = auth.currentUser?.uid ?: return@launch
            val data = mapOf(
                "shopName" to name,
                "ownerName" to owner,
                "phoneNumber" to phone,
                "address" to address
            )
            try {
                firestore.collection("users").document(uid).set(data).await()
            } catch (e: Exception) {
                // Handle error implicitly
            }
        }
    }
    
    fun saveThemeMode(mode: String) {
        viewModelScope.launch {
            settingsManager.saveThemeMode(mode)
        }
    }
    
    fun saveLanguage(lang: String) {
        viewModelScope.launch {
            settingsManager.saveLanguage(lang)
        }
    }
    
    fun saveSecuritySettings(enabled: Boolean, pin: String, requireAuth: Boolean) {
        viewModelScope.launch {
            settingsManager.saveSecuritySettings(enabled, pin, requireAuth)
        }
    }
    
    fun saveNotificationSettings(payment: Boolean, overdue: Boolean, daily: Boolean, weekly: Boolean, backup: Boolean) {
        viewModelScope.launch {
            settingsManager.saveNotificationSettings(payment, overdue, daily, weekly, backup)
        }
    }
    
    fun backupData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            onComplete(true)
        }
    }
    
    fun restoreData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            onComplete(true)
        }
    }
    
    fun deleteAllData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                onComplete(true)
            } catch(e: Exception) {
                onComplete(false)
            }
        }
    }
    
    fun deleteAccount(password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null && user.email != null) {
                try {
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, password)
                    user.reauthenticate(credential).await()
                    user.delete().await()
                    settingsManager.clearData()
                    onComplete(true)
                } catch(e: Exception) {
                    onComplete(false)
                }
            } else {
                onComplete(false)
            }
        }
    }
    
    fun logout() {
        auth.signOut()
        viewModelScope.launch {
            settingsManager.clearData()
        }
    }
    fun saveTimezone(timezone: String) = viewModelScope.launch { settingsManager.saveTimezone(timezone) }
    fun saveDateFormat(format: String) = viewModelScope.launch { settingsManager.saveDateFormat(format) }
}


