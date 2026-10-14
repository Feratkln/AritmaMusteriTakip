package com.aritma.musteritakip.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Tarih ve saat biçimlendirme, ay ve yıl ekleme/hesaplama işlemlerini
 * Türkçe yerel ayarlara (tr-TR) uygun şekilde gerçekleştiren yardımcı sınıf.
 */
object DateUtils {

    private val turkishLocale = Locale("tr", "TR")
    private val displayDateFormat = SimpleDateFormat("dd MMMM yyyy", turkishLocale)
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", turkishLocale)
    private val isoFileFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatDate(millis: Long): String {
        return displayDateFormat.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        return dateTimeFormat.format(Date(millis))
    }

    fun formatFileDate(millis: Long = System.currentTimeMillis()): String {
        return isoFileFormat.format(Date(millis))
    }

    fun addMonths(currentMillis: Long, months: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentMillis
        calendar.add(Calendar.MONTH, months)
        return calendar.timeInMillis
    }

    fun addYears(currentMillis: Long, years: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentMillis
        calendar.add(Calendar.YEAR, years)
        return calendar.timeInMillis
    }
}
