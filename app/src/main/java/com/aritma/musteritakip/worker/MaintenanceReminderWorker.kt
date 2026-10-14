package com.aritma.musteritakip.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aritma.musteritakip.AritmaApplication
import com.aritma.musteritakip.R
import com.aritma.musteritakip.ui.main.MainActivity
import com.aritma.musteritakip.util.MaintenanceStatus
import com.aritma.musteritakip.util.StatusType

/**
 * Arka planda periyodik olarak çalışan WorkManager servis sınıfı.
 * Veritabanındaki aktif müşterilerin bakım tarihlerini kontrol eder;
 * geciken veya yaklaşan bakımı olan müşteriler için durum çubuğuna sistem bildirimi gönderir.
 */
class MaintenanceReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = context.applicationContext as AritmaApplication
            val repository = app.repository
            val customers = repository.getAllActiveCustomersList()

            val urgentCustomers = customers.filter { customer ->
                val status = MaintenanceStatus.calculate(customer.maintenanceDate, customer.isMuted)
                status.type == StatusType.OVERDUE || status.type == StatusType.DUE_SOON
            }

            if (urgentCustomers.isNotEmpty()) {
                sendNotification(urgentCustomers.size)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun sendNotification(urgentCount: Int) {
        val channelId = "maintenance_reminder_channel"
        val notificationId = 1001

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bakım Hatırlatmaları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Günü yaklaşan veya geçen su arıtma bakımları için günlük bildirimler"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("WATERNEON Bakım Hatırlatması 🔧")
            .setContentText("Bugün bakımı gelen veya geçmiş olan $urgentCount müşteri var!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}
