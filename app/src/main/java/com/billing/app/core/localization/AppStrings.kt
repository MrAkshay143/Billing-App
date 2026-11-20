package com.billing.app.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

abstract class AppStrings {
    abstract fun unitSymbol(unitCode: String): String

    abstract val navHome: String
    abstract val navProducts: String
    abstract val navInvoices: String
    abstract val navReports: String
    abstract val navMore: String
    abstract val appSubtitle: String
    abstract val quickAddProduct: String
    abstract val quickProducts: String
    abstract val quickInvoices: String
    abstract val quickPrint: String
    abstract val cartTitle: String
    abstract val cartEmpty: String
    abstract val cartEmptySub: String
    abstract val clearCart: String
    abstract val checkout: String
    abstract val productsTitle: String
    abstract val productsSubtitle: String
    abstract val searchProducts: String
    abstract val filterAll: String
    abstract val filterLowStock: String
    abstract val filterOutOfStock: String
    abstract val invoicesTitle: String
    abstract val invoicesSubtitle: String
    abstract val tabCompleted: String
    abstract val tabDrafts: String
    abstract val tabAll: String
    abstract val searchInvoices: String
    abstract val moreTitle: String
    abstract val moreSubtitle: String
    abstract val groupBusiness: String
    abstract val groupDataReports: String
    abstract val groupApp: String
    abstract val storeProfile: String
    abstract val userAccess: String
    abstract val printerSetup: String
    abstract val paymentSettings: String
    abstract val salesReport: String
    abstract val backupRestore: String
    abstract val appearance: String
    abstract val language: String
    abstract val about: String
    abstract val overview: String
    abstract val totalSales: String
    abstract val totalInvoices: String
    abstract val pendingBill: String
    abstract val totalProducts: String
    abstract val totalCustomers: String
    abstract val scanQrBarcode: String
    abstract val scanSubtitle: String
    abstract val manageYourStore: String
    abstract val heroSubtitle: String
    abstract val newSalePos: String
    abstract val storeManagement: String
    abstract val toolsUtilities: String
    abstract val businessSupport: String
    abstract val categories: String
    abstract val customers: String
    abstract val suppliers: String
    abstract val expenses: String
    abstract val discounts: String
    abstract val taxGst: String
    abstract val paymentMethods: String
    abstract val settings: String
    abstract val barcodeScanner: String
    abstract val importExport: String
    abstract val backupDataRestore: String
    abstract val syncData: String
    abstract val helpSupport: String
    abstract val whatsappSupport: String
    abstract val rateApp: String
    abstract val shareApp: String
    abstract val notifications: String
    abstract val aboutApp: String
    abstract val privacyPolicy: String
    abstract val termsConditions: String
    abstract val logout: String
    abstract val walkInCustomer: String
    abstract val addCustomer: String
    abstract val holdBill: String
    abstract val discardBill: String
    abstract val proceedToPay: String
    abstract val addDiscount: String
    abstract val grandTotal: String
    abstract val subtotal: String
    abstract val inStock: String
    abstract val items: String
    abstract val scanAddItems: String
    abstract val scanSubtitleFull: String
    abstract val scannedItems: String
    abstract val itemAddedToCart: String
    abstract val pointAtBarcode: String
    abstract val done: String
    abstract val add: String
    abstract val newInvoice: String
    abstract val allInvoices: String
    abstract val sales: String
    abstract val purchase: String
    abstract val returns: String
    abstract val paid: String
    abstract val pending: String
    abstract val salesTrend: String
    abstract val topSellingItems: String
    abstract val salesByCategory: String
    abstract val quickReports: String
    abstract val save: String
    abstract val cancel: String
    abstract val delete: String
    abstract val edit: String
    abstract val total: String
    abstract val confirm: String
    abstract val close: String
    abstract val ok: String
    abstract val apply: String
    abstract val clearAll: String
    abstract val reprint: String
    abstract val copy: String
    abstract val grantPermission: String
    abstract val openSettings: String
    abstract val noRecordsFound: String
    abstract val customerKhata: String
    abstract val cash: String
    abstract val upiOnline: String
    abstract val otherCard: String
    abstract val paymentMode: String
    abstract val receivedAmount: String
    abstract val changeToReturn: String
    abstract val exactAmount: String
    abstract val parkAsDraft: String
    abstract val printAndSave: String
    abstract val billSummary: String
    abstract val customerDetailsOptional: String
    abstract val billCompleted: String
    abstract val billSavedInventoryDeducted: String
    abstract val cartEmptyError: String
    abstract val udhaarCustomerRequired: String
    abstract val billParkedToast: String
    abstract val saleCompletedSuccess: String
    abstract val addNewProductTitle: String
    abstract val editProductTitle: String
    abstract val productCatalogSubtitle: String
    abstract val updatePriceStockSubtitle: String
    abstract val productNameLabel: String
    abstract val barcodeLabel: String
    abstract val categoryLabel: String
    abstract val sellingPriceLabel: String
    abstract val purchasePriceLabel: String
    abstract val stockQuantityLabel: String
    abstract val unitLabel: String
    abstract val quickStockAdjust: String
    abstract val currentStock: String
    abstract val addStock: String
    abstract val reduceStock: String
    abstract val deleteProductTitle: String
    abstract val deleteProductConfirm: String
    abstract val errorProductNameRequired: String
    abstract val errorBarcodeRequired: String
    abstract val errorPriceGreaterThanZero: String
    abstract val errorBarcodeExists: String
    abstract val productSavedToast: String
    abstract val stockUpdatedToast: String
    abstract val productDeletedToast: String
    abstract val itemOutOfStockToast: String
    abstract val statusTabPaid: String
    abstract val statusTabPending: String
    abstract val statusTabDrafts: String
    abstract val statusTabReturned: String
    abstract val invoiceHash: String
    abstract val billedTo: String
    abstract val itemsCount: String
    abstract val badgePaid: String
    abstract val badgePending: String
    abstract val badgeDraft: String
    abstract val badgeReturned: String
    abstract val voidInvoiceTitle: String
    abstract val voidInvoicePrompt: String
    abstract val settleDueTitle: String
    abstract val settleDuePrompt: String
    abstract val invoicePrintedToast: String
    abstract val periodToday: String
    abstract val periodThisWeek: String
    abstract val periodThisMonth: String
    abstract val periodThisYear: String
    abstract val salesOverview: String
    abstract val netSales: String
    abstract val grossSales: String
    abstract val totalProfit: String
    abstract val customerKhataReport: String
    abstract val dailyBusinessSummary: String
    abstract val customersTitle: String
    abstract val editCustomer: String
    abstract val customerName: String
    abstract val customerPhone: String
    abstract val customerAddress: String
    abstract val totalDueKhata: String
    abstract val customerSavedToast: String
    abstract val customerDeletedToast: String
    abstract val deleteCustomerTitle: String
    abstract val deleteCustomerConfirm: String
    abstract val suppliersTitle: String
    abstract val addSupplier: String
    abstract val editSupplier: String
    abstract val supplierAgencyName: String
    abstract val contactPerson: String
    abstract val supplierSavedToast: String
    abstract val supplierDeletedToast: String
    abstract val deleteSupplierTitle: String
    abstract val deleteSupplierConfirm: String
    abstract val expensesTitle: String
    abstract val addExpense: String
    abstract val expenseTitle: String
    abstract val expenseAmount: String
    abstract val expenseCategory: String
    abstract val totalExpenses: String
    abstract val expenseSavedToast: String
    abstract val expenseDeletedToast: String
    abstract val deleteExpenseTitle: String
    abstract val deleteExpenseConfirm: String
    abstract val categoriesTitle: String
    abstract val addCategory: String
    abstract val categoryName: String
    abstract val categorySavedToast: String
    abstract val categoryDeletedToast: String
    abstract val deleteCategoryTitle: String
    abstract val deleteCategoryConfirm: String
    abstract val storeInformation: String
    abstract val storeName: String
    abstract val storePhone: String
    abstract val storeAddress: String
    abstract val storeGstin: String
    abstract val receiptFooter: String
    abstract val profileUpdatedToast: String
    abstract val thermalPrinterSetup: String
    abstract val testPrint: String
    abstract val testPrintSuccess: String
    abstract val testPrintFailed: String
    abstract val securityPin: String
    abstract val enterOwnerPin: String
    abstract val enterSecurityPinPrompt: String
    abstract val incorrectPinError: String
    abstract val backupDatabase: String
    abstract val restoreDatabase: String
    abstract val restoreConfirmPrompt: String
    abstract val backupSuccessToast: String
    abstract val restoreSuccessToast: String
    abstract val logoutConfirmTitle: String
    abstract val logoutConfirmPrompt: String
    abstract val howToScanBarcodes: String
    abstract val scanBarcode: String
    abstract val pointCameraAtBarcode: String
    abstract val cameraPermissionNeeded: String
    abstract val barcodeCapturedAuto: String
    abstract val balanceDueKhata: String
    abstract val finishAndBill: String
    abstract val grandTotalLabel: String
    abstract val totalPaidLabel: String
    abstract val cashPaidLabel: String
    abstract val upiPaidLabel: String
    abstract val cashReceivedInput: String
    abstract val cashAmountInput: String
    abstract val upiAmountInput: String
    abstract val amountPaidNowInput: String
    abstract val changeToReturnCash: String
    abstract val payableAmountLabel: String
    abstract val scanWithApps: String
    abstract val setShopUpiPrompt: String
    abstract val holdBillOrSaveDraft: String
    abstract val viewHeldDrafts: String
    abstract val discountMinus: String
    abstract val flatRupees: String
    abstract val percentOff: String
    abstract val creditLimitExceededPrompt: String
    abstract val currentKhataLabel: String
    abstract val creditLimitLabel: String
    abstract val newCustomer: String
    abstract val printingThermalBill: String
    abstract val billPrintedSuccess: String
    abstract val printerSettings: String
    abstract val whatsApp: String
    abstract val clearCartConfirmTitle: String
    abstract val lineDiscountApplied: String
    abstract val openRegisterShift: String
    abstract val enterOpeningFloatPrompt: String
    abstract val openingCashFloatInput: String
    abstract val openShift: String
    abstract val closeShift: String
    abstract val lowStockAlert: String
    abstract val loginError: String
    abstract val opening: String
    abstract val closeShiftAndGenerateZReport: String
    abstract val closeCashDrawerTitle: String
    abstract val cashDrawerActive: String
    abstract val drawerClosed: String
    abstract val countCashPrompt: String
    abstract val countedCashInput: String
    abstract val closingNoteOptional: String
    abstract val closingAndGenerating: String
    abstract val shiftZReportTitle: String
    abstract val cashReconciliation: String
    abstract val printZReportThermal: String
    abstract val managerPin: String
    abstract val fourToSixDigitPin: String
    abstract val authorize: String
    abstract val verifying: String
    abstract val verify: String
    abstract val printing: String
    abstract val selectUserProfile: String
    abstract val chooseStaffAccount: String
    abstract val enterSecurityPin: String
    abstract val accountLockedPrompt: String
    abstract val noActiveUsersFound: String
    abstract val loggingIn: String
    abstract val newGrnTitle: String
    abstract val recordIncomingStockSubtitle: String
    abstract val selectSupplier: String
    abstract val chooseSupplier: String
    abstract val selectProduct: String
    abstract val chooseProduct: String
    abstract val searchProductPlaceholder: String
    abstract val noMatchingProductsFound: String
    abstract val batchDetailsTitle: String
    abstract val expiryDateLabel: String
    abstract val mrpLabel: String
    abstract val addItemToGrn: String
    abstract val itemsInGrn: String
    abstract val noItemsAddedYet: String
    abstract val saveGrn: String
    abstract val saving: String
    abstract val noPurchasesRecorded: String
    abstract val tapNewGrnPrompt: String
    abstract val noSuppliersFoundPrompt: String
    abstract val taxCalculationMode: String
    abstract val supplierAndInvoiceInfo: String
    abstract val addCustomItemTitle: String
    abstract val itemName: String
    abstract val itemNameRequired: String
    abstract val unitPriceOverride: String
    abstract val lineDiscountLabel: String
    abstract val reactivateProductTitle: String
    abstract val reactivateCustomerTitle: String
    abstract val reactivateAndSelect: String
    abstract val reactivate: String
    abstract val locked: String
    abstract val role: String
    abstract val receiptLanguageTitle: String
    abstract val receiptLanguageSubtitle: String
    abstract val availableLanguages: String
    abstract val languagePreferences: String
    abstract val selectPreferredLanguage: String

    // More & Subscreens
    abstract val appearanceTheme: String
    abstract val themeDisplay: String
    abstract val selectApplicationTheme: String
    abstract val systemDefault: String
    abstract val lightMode: String
    abstract val darkMode: String

    abstract val printerConnected: String
    abstract val noPrinterConnected: String
    abstract val disconnect: String
    abstract val receiptPaperFormat: String
    abstract val pairedDevices: String
    abstract val scanPrinters: String
    abstract val noPairedPrinters: String
    abstract val connected: String
    abstract val tapToConnect: String
    abstract val hardwareVerification: String
    abstract val printTestReceipt: String
    abstract val printingSample: String
    abstract val openBluetoothSettings: String

    abstract val upiQrSetup: String
    abstract val storeUpiId: String
    abstract val defaultPaymentMethod: String
    abstract val gstTaxConfig: String
    abstract val enableGstBilling: String
    abstract val defaultTaxRate: String
    abstract val saveSettings: String

    abstract val createLocalBackup: String
    abstract val exportToFile: String
    abstract val importFile: String
    abstract val savedBackups: String
    abstract val noSavedBackups: String
    abstract val deleteBackupTitle: String

    abstract val ownerPinLock: String
    abstract val changePin: String
    abstract val setPin: String
    abstract val removePin: String
    abstract val staffAndCashiers: String
    abstract val addStaffMember: String
    abstract val fullName: String
    abstract val phoneNumber: String
    abstract val accessRole: String
    abstract val removeStaffTitle: String

    abstract val saveStoreProfile: String
    abstract val receiptCustomization: String
    abstract val receiptFooterMessage: String
    abstract val addressLine1: String
    abstract val addressLine2: String
    abstract fun attemptsRemaining(count: Int): String
    abstract fun accountLockedSeconds(seconds: Long): String
    abstract fun fractionalNotAllowed(unit: String): String
    abstract fun unitChangeNotAllowed(productName: String, reason: String): String
    abstract fun invalidUnit(rawUnit: String): String

    abstract val editCategory: String
    abstract val noCategoriesFound: String
    abstract val customImage: String
    abstract val uploadCustomPhoto: String
    abstract val categoryVisual: String
    abstract val customImageSelected: String
    abstract val shownOnCategoryCard: String
    abstract val choosePresetIcon: String
    abstract fun itemCategoriesCount(count: Int): String

    abstract val noCustomersFound: String
    abstract val noPhone: String
    abstract val khataAllClear: String
    abstract fun dueKhata(amount: String): String
    abstract fun advanceKhata(amount: String): String

    abstract val noSuppliersFound: String
    abstract fun supplierContact(person: String): String

    abstract val noExpensesFound: String
    abstract val periodAllTime: String

    abstract val noProductsFound: String
    abstract val adjustStock: String
    abstract val productPhotoOptional: String
    abstract val photoAttached: String
    abstract val retake: String
    abstract val remove: String
    abstract val takePhoto: String
    abstract val fromGallery: String
    abstract val unitLockedInUse: String
    abstract val pricingGstBatchDetails: String
    abstract val gstRate: String
    abstract val enableBatchFefo: String
    abstract val batchTrackingDesc: String
    abstract fun productsInCatalog(count: Int): String
    abstract fun lowStockColon(stock: String): String
    abstract fun inStockColon(stock: String): String

    abstract val salesReturn: String
    abstract val noInvoicesFound: String
    abstract val returnQuantity: String
    abstract val returnReason: String
    abstract val confirmReturn: String
    abstract val totalRevenue: String
    abstract val avgBillValue: String
    abstract val discountsGiven: String
    abstract val cashCollected: String
    abstract val upiPayments: String
    abstract val copyReport: String
    abstract val viewInvoices: String

    abstract val wholesaleConversionTitle: String
    abstract val wholesalePackagingUnit: String
    abstract val unitsPerPackage: String
    abstract val packageCount: String
    abstract val pricePerPackage: String
    abstract val applyWholesaleToItem: String
    abstract val customUnitsTitle: String
    abstract val addCustomUnit: String
    abstract val baseUnit: String
    abstract val conversionFactorLabel: String
    abstract val wholesaleConversionHelp: String

    abstract val receiptInvoice: String
    abstract val receiptDate: String
    abstract val receiptCashier: String
    abstract val receiptCustomer: String
    abstract val receiptPhone: String
    abstract val receiptItemDetails: String
    abstract val receiptTotal: String
    abstract val receiptSubtotal: String
    abstract val receiptDiscount: String
    abstract val receiptTax: String
    abstract val receiptRoundOff: String
    abstract val receiptTotalPayable: String
    abstract val receiptPayment: String
    abstract val receiptCashReceived: String
    abstract val receiptChangeReturned: String
    abstract val receiptUpiPaid: String
    abstract val receiptDueBalance: String
    abstract val receiptThankYou: String
    abstract val totalLineCost: String

    // Phase L1 Centralization additions
    abstract val receivePaymentTitle: String
    abstract val receivePaymentAction: String
    abstract val confirmReceivePayment: String
    abstract val customerDueBalance: String
    abstract val customerKhataStatement: String
    abstract val customerOpeningBalance: String
    abstract val actionSendWhatsApp: String
    abstract val actionCallCustomer: String
    abstract val paymentNotePlaceholder: String

    abstract val placeholderProductName: String
    abstract val placeholderBarcode: String
    abstract val placeholderSku: String
    abstract val placeholderHsn: String
    abstract val placeholderLowStockThreshold: String
    abstract val placeholderCategoryName: String
    abstract val placeholderCustomerName: String
    abstract val placeholderCustomerPhone: String
    abstract val placeholderCustomerAddress: String
    abstract val placeholderOpeningBalance: String
    abstract val placeholderSupplierAgency: String
    abstract val placeholderSupplierContact: String
    abstract val placeholderSupplierPhone: String
    abstract val placeholderSupplierGstin: String
    abstract val placeholderSupplierAddress: String
    abstract val placeholderExpenseTitle: String
    abstract val placeholderExpenseAmount: String

    abstract fun deleteProductPrompt(name: String): String
    abstract fun deleteCustomerPrompt(name: String): String
    abstract fun deleteSupplierPrompt(name: String): String
    abstract fun deleteCategoryPrompt(name: String): String
    abstract fun deleteExpensePrompt(name: String): String

    abstract val stockAdjustmentTitle: String
    abstract val stockAdjustedSuccess: String
    abstract val enterValidQuantity: String
    abstract val cameraErrorToast: String
    abstract val galleryErrorToast: String

    abstract val customItem: String
    abstract val splitPayment: String
    abstract val purchaseGstInclusive: String
    abstract val gstInclusiveShort: String
    abstract val gstExclusiveShort: String
    abstract val placeholderPurchaseInvoice: String
    abstract val placeholderPurchaseQty: String
    abstract val placeholderPurchasePrice: String
    abstract val placeholderBatchNumber: String
    abstract val purchaseItemRequired: String
    abstract val supplierRequired: String
    abstract val invoicesTabAll: String
    abstract val invoicesTabSales: String
    abstract val invoicesTabDrafts: String
    abstract val invoicesTabReturns: String
    abstract val kpiTodaySales: String
    abstract val kpiInvoicesCount: String
    abstract val kpiPendingBills: String
    abstract val kpiTotalItems: String
    abstract val overviewToday: String
    abstract val overviewThisWeek: String
    abstract val overviewThisMonth: String
    abstract val overviewThisYear: String
    abstract val offlineModeBanner: String
    abstract val offlineModeSubtitle: String

    abstract val realCogsHeader: String
    abstract val shiftReconciliationTitle: String
    abstract val shiftCashFlowTitle: String
    abstract val shiftTurnoverTitle: String
    abstract val shiftCashierPerformance: String
    abstract val shiftExpectedCash: String
    abstract val shiftActualCash: String
    abstract val shiftVarianceBalanced: String
    abstract fun shiftVarianceShortage(amount: String): String
    abstract fun shiftVarianceExcess(amount: String): String
    abstract val shiftOpeningFloat: String
    abstract val shiftCashSales: String
    abstract val shiftKhataCollections: String
    abstract val shiftCashExpenses: String
    abstract val shiftCashSupplierPaid: String
    abstract val managerAuthorizationTitle: String
    abstract val managerAuthSubtitle: String
    abstract val aboutDokandaarTitle: String
    abstract val aboutDokandaarDesc: String
    abstract val privacyPolicyTitle: String
    abstract val privacyPolicyDesc: String
    abstract val termsConditionsTitle: String
    abstract val termsConditionsDesc: String
    abstract val importExportTitle: String
    abstract val importExportSubtitle: String
    abstract val placeholderStoreName: String
    abstract val placeholderGstin: String
    abstract val placeholderUpiId: String
    abstract val placeholderUpiName: String
    abstract val cameraPermissionRequired: String
    abstract val cdCamera: String
    abstract val cdGallery: String
    abstract val cdBack: String
    abstract val cdSearch: String
    abstract val cdFilter: String
    abstract val cdOptions: String
    abstract val cdCall: String

    abstract val searchCustomers: String
    abstract val searchSuppliers: String
    abstract val optional: String
    abstract val notes: String
    abstract val enterQuantity: String
}

val EnStrings: AppStrings = EnglishStrings()
val BnStrings: AppStrings = BengaliStrings()
val HiStrings: AppStrings = HindiStrings()

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
