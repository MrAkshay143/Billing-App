# 🏪 Dokandaar — 100% Offline Android Billing & POS System

A high-performance, modern native Android billing and point-of-sale (POS) application built with **Jetpack Compose**, **Room Database**, **CameraX & ML Kit Barcode Scanning**, and **ESC/POS Bluetooth Thermal Printing**.

Designed for retailers and shopkeepers with 100% offline-first reliability.

---

## ✨ Features

- **⚡ Fast POS & Barcode Scanner:** Real-time camera barcode scanning via Google ML Kit with audio and haptic feedback.
- **📄 Draft & Parked Invoices:** Hold customer carts as drafts without decrementing inventory. Resume or discard drafts with one tap.
- **🖨️ Bluetooth Thermal Printing:** Direct ESC/POS printing over Bluetooth for 58mm and 80mm thermal receipt printers.
- **📊 Offline Analytics & KPI Dashboard:** Sales metrics, total revenue, average bill value, and sales reports strictly isolated to completed transactions.
- **🔔 Offline Stock & Sales Notifications:** Automated low-stock alerts when inventory drops below 5 units and daily sales summary notifications.
- **🎨 Dynamic Theme & Multi-Language Support:** OLED Dark Mode, Classic Light Mode, and system-adaptive themes with English, Bengali (বাংলা), and Hindi (हिंदी) localization.
- **💾 Encrypted Backup & Restore:** Full offline JSON database backup and restoration for products, invoices, and line-item history.
- **🛡️ Concurrency & Debounce Protection:** Debounced click listeners and state locking to prevent accidental double-tap billing or race conditions.

---

## 🛠️ Tech Stack & Architecture

- **Language:** Kotlin 2.2.x
- **UI Toolkit:** Jetpack Compose (Material 3)
- **Local Persistence:** Room Database v2 (SQLite) & Preferences DataStore
- **Barcode & Vision:** CameraX + Google ML Kit Barcode Scanning
- **Printing:** Raw Bluetooth RFCOMM socket streaming ESC/POS bytecode
- **Concurrency:** Kotlin Coroutines & Reactive StateFlow
- **Minimum SDK:** Android 7.0 (API 24) | **Target SDK:** Android 16 (API 36)

---

## 🚀 Building & Running

### Prerequisites
- JDK 17+
- Android SDK (API 36)

### Compile Debug Build
```bash
./gradlew assembleDebug
```

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

### Assemble Release Build
```bash
./gradlew assembleRelease
```
The optimized release APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

---

## 📄 License

Proprietary / Private — Dokandaar POS.
