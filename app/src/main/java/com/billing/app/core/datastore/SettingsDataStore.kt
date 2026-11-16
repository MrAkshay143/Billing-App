package com.billing.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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

    suspend fun getSavedPrinterMac(): String? {
        return context.dataStore.data.map { it[KEY_PRINTER_MAC] }.first()
    }

    suspend fun getSavedPrinterName(): String? {
        return context.dataStore.data.map { it[KEY_PRINTER_NAME] }.first()
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
}
