package com.aritma.musteritakip

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aritma.musteritakip.data.local.AppDatabase
import com.aritma.musteritakip.data.repository.CustomerRepository
import com.aritma.musteritakip.worker.MaintenanceReminderWorker
import java.util.concurrent.TimeUnit

/**
 * Uygulamanın yaşam döngüsünü yöneten ana Application sınıfı.
 * Veritabanı ve Repository bağımlılıklarını tekil (singleton) olarak sunar,
 * tema modunu ve periyodik arka plan bakım hatırlatıcılarını başlatır.
 */
class AritmaApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { CustomerRepository(database.customerDao(), database.noteDao(), database.districtDao()) }

    override fun onCreate() {
        super.onCreate()

        // Force consistent bright theme rendering so system Dark Mode never causes faded/inverted text bugs
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setupMaintenanceWorkManager()
    }

    private fun setupMaintenanceWorkManager() {
        val reminderRequest = PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyMaintenanceReminder",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
