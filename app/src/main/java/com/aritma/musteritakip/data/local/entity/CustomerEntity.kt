package com.aritma.musteritakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Müşteri bilgilerini temsil eden Room veritabanı varlık (entity) sınıfı.
 * Bireysel ve kurumsal (lojistik) müşterilerin iletişim, adres, cihaz montaj,
 * bakım tarihleri, susturma ve rezervasyon verilerini saklar.
 */
@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val module: String = MODULE_BIREYSEL, // "bireysel" or "lojistik"
    val name: String,
    val surname: String,
    val phone: String,
    val address: String,
    val district: String,
    val installationDate: Long,
    val maintenanceDate: Long,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    val reservationDate: Long? = null,
    val reservationNote: String? = null,
    val companyName: String? = null,
    val companyPhone: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MODULE_BIREYSEL = "bireysel"
        const val MODULE_LOJISTIK = "lojistik"
        const val MODULE_UV_LAMBA = "uv_lamba"
        const val MODULE_MINERAL = "mineral"
    }

    val fullName: String
        get() = "$name $surname".trim()
}
