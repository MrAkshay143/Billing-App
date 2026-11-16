package com.billing.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.billing.app.core.util.SecurityUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "billing_settings")

class SettingsDataStore(private val context: Context) {
    val appContext: Context = context.applicationContext

    companion object {
        private val KEY_PRINTER_MAC = stringPreferencesKey("printer_mac")
        private val KEY_PRINTER_NAME = stringPreferencesKey("printer_name")
        private val KEY_PRINTER_PAPER_WIDTH = stringPreferencesKey("printer_paper_width")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_APP_THEME = stringPreferencesKey("app_theme")
        private val KEY_STAFF_LIST_JSON = stringPreferencesKey("staff_list_json")
        private val KEY_DEFAULT_PAYMENT_METHOD = stringPreferencesKey("default_payment_method")

        // PIN Security
        private val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        private val KEY_AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")

        // GST / Tax
        private val KEY_GST_ENABLED = booleanPreferencesKey("gst_enabled")
        private val KEY_GSTIN = stringPreferencesKey("gstin")
        private val KEY_TAX_RATE = stringPreferencesKey("tax_rate") // e.g. "0", "5", "12", "18"
    }

    val printerMacFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PRINTER_MAC]
    }

    val printerNameFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_PRINTER_NAME]
    }

    val paperWidthFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PRINTER_PAPER_WIDTH] ?: "58mm"
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_LANGUAGE] ?: "en"
    }

    val appThemeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_APP_THEME] ?: "system"
    }

    val staffListJsonFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_STAFF_LIST_JSON]
    }

    val defaultPaymentMethodFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_DEFAULT_PAYMENT_METHOD] ?: "UPI"
    }

    // PIN Flows
    val pinEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_PIN_ENABLED] ?: false
    }

    val hasPinSetFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        !preferences[KEY_PIN_HASH].isNullOrBlank()
    }

    val autoLockMinutesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_LOCK_MINUTES] ?: 5
    }

    // GST Flows
    val gstEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_GST_ENABLED] ?: false
    }

    val gstinFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_GSTIN] ?: ""
    }

    val taxRateFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_TAX_RATE] ?: "0"
    }

    suspend fun getSavedPrinterMac(): String? {
        return context.dataStore.data.map { it[KEY_PRINTER_MAC] }.first()
    }

    suspend fun getSavedPrinterName(): String? {
        return context.dataStore.data.map { it[KEY_PRINTER_NAME] }.first()
    }

    suspend fun getPaperWidth(): String {
        return context.dataStore.data.map { it[KEY_PRINTER_PAPER_WIDTH] ?: "58mm" }.first()
    }

    suspend fun savePrinterData(mac: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PRINTER_MAC] = mac
            preferences[KEY_PRINTER_NAME] = name
        }
    }

    suspend fun savePaperWidth(width: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PRINTER_PAPER_WIDTH] = width
        }
    }

    suspend fun clearPrinterData() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PRINTER_MAC)
            preferences.remove(KEY_PRINTER_NAME)
        }
    }

    suspend fun areNotificationsEnabled(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] ?: true
        }.first()
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun saveAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_LANGUAGE] = language
        }
    }

    suspend fun saveAppTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_APP_THEME] = theme
        }
    }

    suspend fun saveStaffListJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_STAFF_LIST_JSON] = json
        }
    }

    suspend fun saveDefaultPaymentMethod(method: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DEFAULT_PAYMENT_METHOD] = method
        }
    }

    // PIN Management
    suspend fun setPin(pin: String) {
        val salt = SecurityUtil.generateSalt()
        val hash = SecurityUtil.hashPin(pin, salt)
        context.dataStore.edit { preferences ->
            preferences[KEY_PIN_SALT] = salt
            preferences[KEY_PIN_HASH] = hash
            preferences[KEY_PIN_ENABLED] = true
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = context.dataStore.data.first()
        val salt = prefs[KEY_PIN_SALT] ?: return false
        val hash = prefs[KEY_PIN_HASH] ?: return false
        return SecurityUtil.verifyPin(pin, salt, hash)
    }

    suspend fun removePin() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PIN_SALT)
            preferences.remove(KEY_PIN_HASH)
            preferences[KEY_PIN_ENABLED] = false
        }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PIN_ENABLED] = enabled
        }
    }

    suspend fun saveAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCK_MINUTES] = minutes
        }
    }

    // GST Management
    suspend fun saveGstSettings(enabled: Boolean, gstin: String, taxRate: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GST_ENABLED] = enabled
            preferences[KEY_GSTIN] = gstin.trim().uppercase()
            preferences[KEY_TAX_RATE] = taxRate.trim()
        }
    }
}
