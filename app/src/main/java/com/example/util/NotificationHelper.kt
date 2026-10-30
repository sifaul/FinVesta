package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PaymentReminderEntity

object NotificationHelper {

    private const val CHANNEL_ID = "finvesta_reminders_channel"
    private const val CHANNEL_NAME = "Pengingat Pembayaran FinVesta"
    private const val CHANNEL_DESC = "Notifikasi pengingat jatuh tempo tagihan dan pembayaran rutin"

    private const val CHANNEL_BUDGET_ID = "finvesta_budget_channel"
    private const val CHANNEL_BUDGET_NAME = "Peringatan Batas Anggaran FinVesta"
    private const val CHANNEL_BUDGET_DESC = "Notifikasi peringatan saat pengeluaran bulanan kategori melebihi 80% dari batas anggaran"

    private const val CHANNEL_SECURITY_ID = "finvesta_security_channel"
    private const val CHANNEL_SECURITY_NAME = "Keamanan & Pemulihan PIN FinVesta"
    private const val CHANNEL_SECURITY_DESC = "Kode verifikasi dan pemulihan akses akun FinVesta"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val reminderChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val budgetChannel = NotificationChannel(CHANNEL_BUDGET_ID, CHANNEL_BUDGET_NAME, importance).apply {
                description = CHANNEL_BUDGET_DESC
                enableVibration(true)
            }
            val securityChannel = NotificationChannel(CHANNEL_SECURITY_ID, CHANNEL_SECURITY_NAME, importance).apply {
                description = CHANNEL_SECURITY_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(budgetChannel)
            notificationManager.createNotificationChannel(securityChannel)
        }
    }

    fun sendPaymentReminderNotification(
        context: Context,
        reminder: PaymentReminderEntity
    ): Boolean {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formattedAmount = CurrencyFormatter.formatRupiah(reminder.amount)
        val relativeDate = DateUtils.getRelativeDueText(reminder.dueDateEpochMillis)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Pengingat: ${reminder.title}")
            .setContentText("Tagihan $formattedAmount jatuh tempo $relativeDate ($reminder.paymentAccount)")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Tagihan ${reminder.title} sebesar $formattedAmount jatuh tempo $relativeDate.\nMetode: ${reminder.paymentAccount}\nCatatan: ${reminder.note.ifEmpty { "Jangan lupa bayar sebelum jatuh tempo agar tidak terkena denda." }}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(reminder.id.toInt().coerceAtLeast(1001), builder.build())
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    // Riwayat pengiriman notifikasi agar tidak harus selalu muncul terus-menerus
    private val lastNotificationTimestamps = mutableMapOf<String, Long>()

    fun sendBudgetAlertNotification(
        context: Context,
        categoryName: String,
        spentAmount: Double,
        budgetLimit: Double,
        percentage: Double
    ): Boolean {
        // Peringatan HANYA muncul saat pengeluaran sudah lebih dari 80%. Jika belum (>80%), jangan munculkan.
        if (percentage <= 80.0) {
            return false
        }

        // Jangan selalu memunculkan notifikasi berulang-ulang untuk kategori yang sama (cooldown 2 jam kecuali telah menembus 100%)
        val statusKey = "${categoryName.lowercase()}_${if (percentage >= 100.0) "EXCEEDED" else "WARNING"}"
        val now = System.currentTimeMillis()
        val lastSent = lastNotificationTimestamps[statusKey] ?: 0L
        if (now - lastSent < 2 * 3600 * 1000L) {
            // Masih dalam cooldown, jangan spam notifikasi
            return false
        }
        lastNotificationTimestamps[statusKey] = now

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notificationId = 2000 + (categoryName.hashCode().let { if (it < 0) -it else it } % 1000)
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val formattedSpent = CurrencyFormatter.formatRupiah(spentAmount)
        val formattedLimit = CurrencyFormatter.formatRupiah(budgetLimit)
        val remaining = (budgetLimit - spentAmount).coerceAtLeast(0.0)
        val formattedRemaining = CurrencyFormatter.formatRupiah(remaining)
        val percentText = String.format(java.util.Locale.getDefault(), "%.1f", percentage)

        val isOver100 = percentage >= 100.0
        val alertTitle = if (isOver100) {
            "🚨 Anggaran Terlampaui: $categoryName"
        } else {
            "⚠️ Peringatan Anggaran: $categoryName ($percentText%)"
        }

        val alertSummary = if (isOver100) {
            "Pengeluaran $formattedSpent telah MELEBIHI batas anggaran $formattedLimit!"
        } else {
            "Pengeluaran $formattedSpent telah mencapai $percentText% dari batas $formattedLimit"
        }

        val bigText = StringBuilder().apply {
            append("Pengeluaran bulanan untuk kategori \"$categoryName\" telah mencapai $percentText% dari batas anggaran yang Anda tentukan.\n\n")
            append("• Terpakai: $formattedSpent\n")
            append("• Batas Anggaran: $formattedLimit\n")
            if (isOver100) {
                val excess = CurrencyFormatter.formatRupiah(spentAmount - budgetLimit)
                append("• Kelebihan: $excess\n\n")
                append("Perhatian: Anda telah melampaui batas anggaran bulanan. Segera batasi pengeluaran tambahan untuk kategori ini.")
            } else {
                append("• Sisa Anggaran: $formattedRemaining\n\n")
                append("Peringatan: Pengeluaran telah melampaui ambang batas 80%. Mohon kontrol pengeluaran agar tidak melebihi anggaran bulanan.")
            }
        }.toString()

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(alertTitle)
            .setContentText(alertSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    fun sendSecurityOtpNotification(
        context: Context,
        otpCode: String,
        email: String
    ): Boolean {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            9901,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationId = 9901
        val title = "🔐 Kode Verifikasi Pemulihan PIN FinVesta"
        val summary = "Kode OTP: $otpCode (Berlaku 10 menit)"
        val bigText = "Kode verifikasi pemulihan akses akun FinVesta Anda adalah:\n\n" +
                "🔑 $otpCode\n\n" +
                "Dikirim untuk: $email\n" +
                "Jangan berikan kode ini kepada siapapun demi keamanan finansial Anda."

        val builder = NotificationCompat.Builder(context, CHANNEL_SECURITY_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
            return true
        } catch (e: SecurityException) {
            return false
        }
    }
}
