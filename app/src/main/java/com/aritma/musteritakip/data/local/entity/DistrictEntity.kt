package com.aritma.musteritakip.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * İlçe ve semt tanımlarını temsil eden Room veritabanı varlık sınıfı.
 * Müşteri filtrelemelerinde ve adres seçimlerinde kullanılır.
 */
@Entity(tableName = "districts")
data class DistrictEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
