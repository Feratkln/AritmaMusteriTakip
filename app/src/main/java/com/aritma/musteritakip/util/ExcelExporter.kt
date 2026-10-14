package com.aritma.musteritakip.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import java.io.File
import java.io.FileOutputStream

/**
 * Müşteri verilerini Excel uyumlu CSV (UTF-8 BOM) formatında dışa aktaran
 * ve Android paylaşım menüsü üzerinden paylaşılmasını sağlayan yardımcı sınıf.
 */
object ExcelExporter {

    fun exportCustomersToCsv(context: Context, customers: List<CustomerEntity>): File? {
        return try {
            val fileName = "musteriler_${DateUtils.formatFileDate()}.csv"
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                // UTF-8 BOM for Excel Turkish character encoding compatibility
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))

                val header = "ID;Modul;Ad;Soyad;Telefon;Semt;Adres;Takilma Tarihi;Bakim Tarihi;Bakim Durumu\n"
                out.write(header.toByteArray(Charsets.UTF_8))

                customers.forEach { customer ->
                    val status = MaintenanceStatus.calculate(customer.maintenanceDate)
                    val line = "${customer.id};" +
                            "${customer.module};" +
                            "\"${customer.name}\";" +
                            "\"${customer.surname}\";" +
                            "\"${customer.phone}\";" +
                            "\"${customer.district}\";" +
                            "\"${customer.address}\";" +
                            "\"${DateUtils.formatDate(customer.installationDate)}\";" +
                            "\"${DateUtils.formatDate(customer.maintenanceDate)}\";" +
                            "\"${status.displayText}\"\n"

                    out.write(line.toByteArray(Charsets.UTF_8))
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareExportedFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Su Arıtma Müşteri Listesi Yedeği - ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Müşteri Listesini Paylaş / Dışa Aktar"))
    }
}
