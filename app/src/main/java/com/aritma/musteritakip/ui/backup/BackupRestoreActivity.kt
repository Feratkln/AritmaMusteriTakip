package com.aritma.musteritakip.ui.backup

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aritma.musteritakip.AritmaApplication
import com.aritma.musteritakip.databinding.ActivityBackupRestoreBinding
import com.aritma.musteritakip.util.ExcelExporter
import com.aritma.musteritakip.util.JsonBackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Veri tabanı yedekleme ve geri yükleme işlemlerini yöneten Activity.
 * Müşteri listesini Excel (.csv) ve JSON formatında dışa aktarma ile
 * daha önce alınmış bir JSON yedeğini veri tabanına geri yükleme işlevlerini sunar.
 */
class BackupRestoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupRestoreBinding

    private val repository by lazy { (application as AritmaApplication).repository }

    // File Picker Launcher for Restoring JSON Backup
    private val restoreFileLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            AlertDialog.Builder(this)
                .setTitle("Veritabanını Geri Yükle")
                .setMessage("DİKKAT: Seçilen yedek dosyası mevcut verilerinizin üzerine yazılacaktır. Devam etmek istiyor musunuz?")
                .setPositiveButton("Evet, Geri Yükle") { _, _ ->
                    restoreBackupFromUri(uri)
                }
                .setNegativeButton("İptal", null)
                .show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupRestoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // 1. Export Excel (.csv) Action
        binding.btnExportExcel.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val customers = repository.getAllActiveCustomersList()
                val file = ExcelExporter.exportCustomersToCsv(this@BackupRestoreActivity, customers)

                withContext(Dispatchers.Main) {
                    if (file != null) {
                        ExcelExporter.shareExportedFile(this@BackupRestoreActivity, file)
                    } else {
                        Toast.makeText(this@BackupRestoreActivity, "Excel dosyası oluşturulamadı!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 2. Export JSON Database Backup Action
        binding.btnExportJsonBackup.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val customers = repository.getAllActiveCustomersList()
                val notes = repository.getAllNotesList()
                val file = JsonBackupManager.createBackupFile(this@BackupRestoreActivity, customers, notes)

                withContext(Dispatchers.Main) {
                    if (file != null) {
                        JsonBackupManager.shareBackupFile(this@BackupRestoreActivity, file)
                    } else {
                        Toast.makeText(this@BackupRestoreActivity, "Yedek dosyası oluşturulamadı!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 3. Restore JSON Database Backup Action
        binding.btnRestoreJsonBackup.setOnClickListener {
            restoreFileLauncher.launch("application/json")
        }
    }

    private fun restoreBackupFromUri(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val backupData = JsonBackupManager.parseBackupUri(this@BackupRestoreActivity, uri)

            withContext(Dispatchers.Main) {
                if (backupData != null && backupData.customers.isNotEmpty()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        repository.restoreDatabase(backupData.customers, backupData.notes)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@BackupRestoreActivity,
                                "Geri yükleme başarılı! ${backupData.customers.size} müşteri yüklendi. ✅",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this@BackupRestoreActivity,
                        "Geçersiz veya boş yedek dosyası!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
