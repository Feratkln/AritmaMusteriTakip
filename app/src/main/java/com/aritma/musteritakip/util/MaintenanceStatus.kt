package com.aritma.musteritakip.util

import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Müşteri bakım tarihlerini ve kalan/geçen gün sayılarını hesaplayıp
 * renk/durum tipine (Geçmiş, Yaklaşan, Güvenli, Susturuldu) dönüştüren veri sınıfı.
 */
enum class StatusType {
    OVERDUE,    // 🔴 Bakımı Geçmiş (< 0 gün)
    DUE_SOON,   // 🟠 Bakımı Yaklaşan (0..15 gün)
    SAFE,       // ⚪ Bakımına Zaman Var (> 15 gün)
    MUTED       // 🔇 Susturuldu / Pasif
}

data class MaintenanceStatus(
    val type: StatusType,
    val daysRemaining: Long
) {
    companion object {
        fun calculate(maintenanceDateMillis: Long, isMuted: Boolean = false): MaintenanceStatus {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val targetCal = Calendar.getInstance().apply {
                timeInMillis = maintenanceDateMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
            val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

            val type = when {
                isMuted -> StatusType.MUTED
                days < 0 -> StatusType.OVERDUE
                days in 0..15 -> StatusType.DUE_SOON
                else -> StatusType.SAFE
            }

            return MaintenanceStatus(type, days)
        }
    }

    val displayText: String
        get() = when (type) {
            StatusType.MUTED -> "🔇 Susturuldu (Bildirim Dışı)"
            StatusType.OVERDUE -> "Bakımı ${Math.abs(daysRemaining)} Gün Geçti!"
            StatusType.DUE_SOON -> if (daysRemaining == 0L) "Bakım Günü BUGÜN!" else "Bakıma $daysRemaining Gün Kaldı"
            StatusType.SAFE -> "Bakıma $daysRemaining Gün Var"
        }
}
