package com.billing.app.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

data class AppStrings(
    // tabs
    val navHome: String,
    val navProducts: String,
    val navInvoices: String,
    val navReports: String,
    val navMore: String,

    // topHeader
    val appSubtitle: String,

    // home
    val quickAddProduct: String,
    val quickProducts: String,
    val quickInvoices: String,
    val quickPrint: String,
    val cartTitle: String,
    val cartEmpty: String,
    val cartEmptySub: String,
    val clearCart: String,
    val checkout: String,

    // products
    val productsTitle: String,
    val productsSubtitle: String,
    val searchProducts: String,
    val filterAll: String,
    val filterLowStock: String,
    val filterOutOfStock: String,

    // invoices
    val invoicesTitle: String,
    val invoicesSubtitle: String,
    val tabCompleted: String,
    val tabDrafts: String,
    val tabAll: String,
    val searchInvoices: String,

    // more
    val moreTitle: String,
    val moreSubtitle: String,
    val groupBusiness: String,
    val groupDataReports: String,
    val groupApp: String,
    val storeProfile: String,
    val userAccess: String,
    val printerSetup: String,
    val paymentSettings: String,
    val salesReport: String,
    val backupRestore: String,
    val appearance: String,
    val language: String,
    val about: String,

    // common
    val save: String,
    val cancel: String,
    val delete: String,
    val edit: String,
    val total: String
)

val EnStrings = AppStrings(
    navHome = "Home",
    navProducts = "Products",
    navInvoices = "Invoices",
    navReports = "Reports",
    navMore = "More",
    appSubtitle = "Billing & POS",
    quickAddProduct = "Add Product",
    quickProducts = "Products",
    quickInvoices = "Invoices",
    quickPrint = "Print",
    cartTitle = "Cart",
    cartEmpty = "Cart is empty",
    cartEmptySub = "Scan products above or use Add Product",
    clearCart = "Clear",
    checkout = "Checkout",
    productsTitle = "Products",
    productsSubtitle = "Manage your store products",
    searchProducts = "Search by name, barcode or QR...",
    filterAll = "All",
    filterLowStock = "Low Stock",
    filterOutOfStock = "Out of Stock",
    invoicesTitle = "Invoices",
    invoicesSubtitle = "View, search and manage sales",
    tabCompleted = "Completed",
    tabDrafts = "Drafts",
    tabAll = "All Invoices",
    searchInvoices = "Search by invoice no. or date...",
    moreTitle = "More",
    moreSubtitle = "Settings, backup, reports and more",
    groupBusiness = "Business",
    groupDataReports = "Data & Reports",
    groupApp = "App",
    storeProfile = "Store Profile",
    userAccess = "User & Access",
    printerSetup = "Printer Setup",
    paymentSettings = "Payment Settings",
    salesReport = "Sales Report",
    backupRestore = "Backup & Restore",
    appearance = "Appearance",
    language = "Language",
    about = "About Dokandaar",
    save = "Save",
    cancel = "Cancel",
    delete = "Delete",
    edit = "Edit",
    total = "Total"
)

val BnStrings = AppStrings(
    navHome = "হোম",
    navProducts = "পণ্যসমূহ",
    navInvoices = "ইনভয়েস",
    navReports = "রিপোর্ট",
    navMore = "আরও",
    appSubtitle = "বিলিং ও পিওএস",
    quickAddProduct = "পণ্য যোগ",
    quickProducts = "পণ্যসমূহ",
    quickInvoices = "ইনভয়েস",
    quickPrint = "প্রিন্টার",
    cartTitle = "কার্ট",
    cartEmpty = "কার্ট খালি আছে",
    cartEmptySub = "পণ্য যোগ করতে বারকোড স্ক্যান করুন বা নির্বাচন করুন",
    clearCart = "সব মুছুন",
    checkout = "বিলিং করুন",
    productsTitle = "পণ্যসমূহ",
    productsSubtitle = "দোকানের পণ্য তালিকা পরিচালনা করুন",
    searchProducts = "নাম, বারকোড বা কিউআর কোড দিয়ে খুঁজুন...",
    filterAll = "সব",
    filterLowStock = "কম স্টক",
    filterOutOfStock = "স্টক শেষ",
    invoicesTitle = "ইনভয়েস",
    invoicesSubtitle = "বিক্রয় তালিকা ও ইনভয়েস পরিচালনা করুন",
    tabCompleted = "সম্পন্ন",
    tabDrafts = "খসড়া",
    tabAll = "সব ইনভয়েস",
    searchInvoices = "ইনভয়েস নম্বর বা তারিখ দিয়ে খুঁজুন...",
    moreTitle = "আরও",
    moreSubtitle = "সেটিংস, ব্যাকআপ, রিপোর্ট এবং অন্যান্য",
    groupBusiness = "ব্যবসা",
    groupDataReports = "তথ্য ও রিপোর্ট",
    groupApp = "অ্যাপ্লিকেশন",
    storeProfile = "দোকানের বিবরণ",
    userAccess = "ইউজার ও পারমিশন",
    printerSetup = "প্রিন্টার সেটআপ",
    paymentSettings = "পেমেন্ট সেটিংস",
    salesReport = "বিক্রয় রিপোর্ট",
    backupRestore = "ব্যাকআপ ও রিস্টোর",
    appearance = "থিম ও ডিসপ্লে",
    language = "ভাষা পরিবর্তন",
    about = "অ্যাপ সম্পর্কে",
    save = "সংরক্ষণ",
    cancel = "বাতিল",
    delete = "মুছুন",
    edit = "সম্পাদনা",
    total = "মোট"
)

val HiStrings = AppStrings(
    navHome = "होम",
    navProducts = "उत्पाद",
    navInvoices = "इनवॉइस",
    navReports = "रिपोर्ट्स",
    navMore = "अन्य",
    appSubtitle = "बिलिंग और पीओएस",
    quickAddProduct = "उत्पाद जोड़ें",
    quickProducts = "उत्पाद सूची",
    quickInvoices = "इनवॉइस",
    quickPrint = "प्रिंटर",
    cartTitle = "कार्ट",
    cartEmpty = "कार्ट खाली है",
    cartEmptySub = "उत्पाद जोड़ने के लिए बारकोड स्कैन करें या चुनें",
    clearCart = "हटाएं",
    checkout = "बिल बनाएं",
    productsTitle = "उत्पाद",
    productsSubtitle = "अपनी दुकान के उत्पादों का प्रबंधन करें",
    searchProducts = "नाम, बारकोड या क्यूआर से खोजें...",
    filterAll = "सभी",
    filterLowStock = "कम स्टॉक",
    filterOutOfStock = "स्टॉक समाप्त",
    invoicesTitle = "इनवॉइस",
    invoicesSubtitle = "बिक्री देखें, खोजें और प्रबंधित करें",
    tabCompleted = "पूर्ण",
    tabDrafts = "ड्राफ्ट",
    tabAll = "सभी इनवॉइस",
    searchInvoices = "इनवॉइस नंबर या तारीख से खोजें...",
    moreTitle = "अन्य",
    moreSubtitle = "सेटिंग्स, बैकअप, रिपोर्ट और अन्य",
    groupBusiness = "व्यवसाय",
    groupDataReports = "डेटा और रिपोर्ट",
    groupApp = "ऐप",
    storeProfile = "दुकान विवरण",
    userAccess = "स्टाफ और परमिशन",
    printerSetup = "प्रिंटर सेटअप",
    paymentSettings = "भुगतान सेटिंग्स",
    salesReport = "बिक्री रिपोर्ट",
    backupRestore = "बैकअप और रीस्टोर",
    appearance = "थीम और डिस्प्ले",
    language = "भाषा बदलें",
    about = "ऐप के बारे में",
    save = "सुरक्षित करें",
    cancel = "रद्द करें",
    delete = "हटाएं",
    edit = "संपादित करें",
    total = "कुल"
)

fun getAppStrings(languageCode: String?): AppStrings {
    return when (languageCode?.lowercase()) {
        "bn" -> BnStrings
        "hi" -> HiStrings
        else -> EnStrings
    }
}

val LocalAppStrings = staticCompositionLocalOf { EnStrings }

@Composable
fun appStrings(): AppStrings = LocalAppStrings.current
