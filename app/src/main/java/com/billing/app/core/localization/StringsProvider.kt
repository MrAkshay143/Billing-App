package com.billing.app.core.localization

import android.content.Context

object StringsProvider {
    @Volatile
    private var _current: AppStrings = EnStrings

    @Volatile
    private var _receiptStrings: AppStrings = EnStrings

    @Volatile
    private var _receiptLang: String = "en"

    val current: AppStrings
        get() = _current

    val receiptStrings: AppStrings
        get() = _receiptStrings

    val receiptLang: String
        get() = _receiptLang

    fun init(context: Context) {
        val sp = context.getSharedPreferences("dokandaar_prefs", Context.MODE_PRIVATE)
        val lang = sp.getString("app_language", "en") ?: "en"
        val receipt = sp.getString("receipt_language", "en") ?: "en"
        _current = getAppStrings(lang)
        _receiptLang = receipt
        _receiptStrings = getAppStrings(receipt)
    }

    fun updateLanguage(langCode: String) {
        _current = getAppStrings(langCode)
    }

    fun updateReceiptLanguage(langCode: String) {
        _receiptLang = langCode
        _receiptStrings = getAppStrings(langCode)
    }

    fun get(langCode: String): AppStrings = getAppStrings(langCode)
}
