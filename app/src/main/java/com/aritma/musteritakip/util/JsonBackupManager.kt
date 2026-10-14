package com.aritma.musteritakip.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.data.local.entity.NoteEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

data class FullBackupData(
    val version: Int = 1,
    val exportDate: String = DateUtils.formatFileDate(),
    val customers: List<CustomerEntity>,
    val notes: List<NoteEntity>
)

/**
 * Müşteri ve servis notları veritabanını tam yedek (.json) olarak dışa aktaran
 * ve JSON yedek dosyasından veritabanına geri yükleyen yardımcı sınıf.
 */
object JsonBackupManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun createBackupFile(context: Context, customers: List<CustomerEntity>, notes: List<NoteEntity>): File? {
        return try {
            val backupData = FullBackupData(
                customers = customers,
                notes = notes
            )
            val jsonString = gson.toJson(backupData)

            val fileName = "aritma_yedek_${DateUtils.formatFileDate()}.json"
            val exportDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseBackupUri(context: Context, uri: Uri): FullBackupData? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).use { reader ->
                    gson.fromJson(reader, FullBackupData::class.java)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareBackupFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Su Arıtma Tam Veritabanı Yedeği - ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Veritabanı Yedeğini Paylaş / Gönder"))
    }
}
