package com.aritma.musteritakip.data.local.dao

import androidx.room.*
import com.aritma.musteritakip.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Müşterilere ait servis ve bakım geçmişi notlarını yöneten DAO arayüzü.
 * Not ekleme, silme ve kronolojik sıralama işlemlerini gerçekleştirir.
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM customer_notes WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun getNotesForCustomer(customerId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM customer_notes WHERE customerId = :customerId ORDER BY timestamp DESC")
    suspend fun getNotesForCustomerList(customerId: Long): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Query("SELECT * FROM customer_notes")
    suspend fun getAllNotes(): List<NoteEntity>

    @Query("DELETE FROM customer_notes")
    suspend fun deleteAll()
}
