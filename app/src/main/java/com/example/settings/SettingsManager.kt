package com.example.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val PHONE_NUMBER = stringPreferencesKey("phone_number")
        val ADDRESS = stringPreferencesKey("address")
        
        val THEME_MODE = stringPreferencesKey("theme_mode") // Light, Dark, System
        val LANGUAGE = stringPreferencesKey("language")
        val TIMEZONE = stringPreferencesKey("timezone")
        val DATE_FORMAT = stringPreferencesKey("date_format") // English, Bangla
        
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val APP_PIN = stringPreferencesKey("app_pin")
        val REQUIRE_AUTH = booleanPreferencesKey("require_auth")
        
        val NOTIFY_PAYMENT = booleanPreferencesKey("notify_payment")
        val NOTIFY_OVERDUE = booleanPreferencesKey("notify_overdue")
        val NOTIFY_DAILY = booleanPreferencesKey("notify_daily")
        val NOTIFY_WEEKLY = booleanPreferencesKey("notify_weekly")
        val NOTIFY_BACKUP = booleanPreferencesKey("notify_backup")
        val OVERDUE_THRESHOLD_DAYS = intPreferencesKey("overdue_threshold_days")
    }

    val shopNameFlow: Flow<String> = context.dataStore.data.map { it[SHOP_NAME] ?: "" }
    val ownerNameFlow: Flow<String> = context.dataStore.data.map { it[OWNER_NAME] ?: "" }
    val phoneNumberFlow: Flow<String> = context.dataStore.data.map { it[PHONE_NUMBER] ?: "" }
    val addressFlow: Flow<String> = context.dataStore.data.map { it[ADDRESS] ?: "" }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "System Default" }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[LANGUAGE] ?: "English" }
    val timezoneFlow: Flow<String> = context.dataStore.data.map { it[TIMEZONE] ?: "System Default" }
    val dateFormatFlow: Flow<String> = context.dataStore.data.map { it[DATE_FORMAT] ?: "dd MMM yyyy" }

    val appLockEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[APP_LOCK_ENABLED] ?: false }
    val appPinFlow: Flow<String> = context.dataStore.data.map { it[APP_PIN] ?: "" }
    val requireAuthFlow: Flow<Boolean> = context.dataStore.data.map { it[REQUIRE_AUTH] ?: false }

    val notifyPaymentFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFY_PAYMENT] ?: true }
    val notifyOverdueFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFY_OVERDUE] ?: true }
    val notifyDailyFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFY_DAILY] ?: false }
    val notifyWeeklyFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFY_WEEKLY] ?: false }
    val notifyBackupFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFY_BACKUP] ?: true }

    val overdueThresholdDaysFlow: Flow<Int> = context.dataStore.data.map { it[OVERDUE_THRESHOLD_DAYS] ?: 15 }

    suspend fun saveShopInfo(name: String, owner: String, phone: String, address: String) {
        context.dataStore.edit { prefs ->
            prefs[SHOP_NAME] = name
            prefs[OWNER_NAME] = owner
            prefs[PHONE_NUMBER] = phone
            prefs[ADDRESS] = address
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun saveLanguage(lang: String) {
        context.dataStore.edit { it[LANGUAGE] = lang }
    }

    suspend fun saveTimezone(timezone: String) {
        context.dataStore.edit { it[TIMEZONE] = timezone }
    }

    suspend fun saveDateFormat(format: String) {
        context.dataStore.edit { it[DATE_FORMAT] = format }
    }

    suspend fun saveSecuritySettings(enabled: Boolean, pin: String, requireAuth: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[APP_LOCK_ENABLED] = enabled
            prefs[APP_PIN] = pin
            prefs[REQUIRE_AUTH] = requireAuth
        }
    }

    suspend fun saveNotificationSettings(payment: Boolean, overdue: Boolean, daily: Boolean, weekly: Boolean, backup: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[NOTIFY_PAYMENT] = payment
            prefs[NOTIFY_OVERDUE] = overdue
            prefs[NOTIFY_DAILY] = daily
            prefs[NOTIFY_WEEKLY] = weekly
            prefs[NOTIFY_BACKUP] = backup
        }
    }
    
    suspend fun saveOverdueThresholdDays(days: Int) {
        context.dataStore.edit { it[OVERDUE_THRESHOLD_DAYS] = days }
    }
    
    suspend fun clearData() {
        context.dataStore.edit { it.clear() }
    }
}
