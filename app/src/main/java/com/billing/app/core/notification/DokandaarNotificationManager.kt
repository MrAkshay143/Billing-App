package com.billing.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.billing.app.MainActivity
import com.billing.app.R

object DokandaarNotificationManager {
    const val CHANNEL_LOW_STOCK = "channel_low_stock"
    const val CHANNEL_SALES_ALERTS = "channel_sales_alerts"

    private const val NOTIFICATION_ID_LOW_STOCK = 1001
    private const val NOTIFICATION_ID_DAILY_SALES = 1002

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Low Stock Alerts Channel
            val lowStockChannel = NotificationChannel(
                CHANNEL_LOW_STOCK,
                "Low Stock Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when products are running low or out of stock"
                enableVibration(true)
            }

            // 2. Daily Sales Channel
            val salesChannel = NotificationChannel(
                CHANNEL_SALES_ALERTS,
                "Daily Sales Summary",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily sales summary and store performance alerts"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(lowStockChannel)
            notificationManager.createNotificationChannel(salesChannel)
        }
    }

    fun showLowStockNotification(
        context: Context,
        productCount: Int,
        sampleNames: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (productCount == 1) {
            "Low Stock Alert: $sampleNames"
        } else {
            "Low Stock Alert ($productCount products)"
        }
        val body = "Inventory running low on: $sampleNames. Tap to review and restock."

        val notification = NotificationCompat.Builder(context, CHANNEL_LOW_STOCK)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setColor(0xFF7629D9.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_LOW_STOCK, notification)
    }

    fun showDailySalesNotification(
        context: Context,
        totalInvoices: Int,
        totalRevenueFormatted: String
    ) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Daily Store Summary"
        val body = "Today: $totalInvoices sales completed | Total Revenue: $totalRevenueFormatted"

        val notification = NotificationCompat.Builder(context, CHANNEL_SALES_ALERTS)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setColor(0xFF7629D9.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY_SALES, notification)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}