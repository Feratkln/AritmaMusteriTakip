package com.aritma.musteritakip.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Müşteriye ait servis notlarını ve işlem geçmişini tutan Room varlık sınıfı.
 * CustomerEntity ile ForeignKey ilişkisine sahiptir.
 */
@Entity(
    tableName = "customer_notes",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val noteText: String,
    val timestamp: Long = System.currentTimeMillis()
)
