package com.aritma.musteritakip.data.local.dao

import androidx.room.*
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

/**
 * Müşteri kayıtlarına yönelik veritabanı işlemlerini yürüten DAO arayüzü.
 * Modül filtreleme, aktif müşteri akışları, bakım güncellemeleri,
 * susturma ve randevu kayıtlarının sorgulanmasını sağlar.
 */
@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE module = :module AND isArchived = 0 ORDER BY name ASC")
    fun getCustomersByModule(module: String): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY maintenanceDate ASC")
    suspend fun getAllActiveCustomersList(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE isArchived = 0 ORDER BY maintenanceDate ASC")
    fun getAllActiveCustomersFlow(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Delete
    suspend fun deleteCustomer(customer: CustomerEntity)

    @Query("UPDATE customers SET maintenanceDate = :newDate WHERE id = :customerId")
    suspend fun updateMaintenanceDate(customerId: Long, newDate: Long)

    @Query("UPDATE customers SET isArchived = 1 WHERE id = :customerId")
    suspend fun archiveCustomer(customerId: Long)

    @Query("SELECT COUNT(*) FROM customers WHERE module = :module AND isArchived = 0")
    fun getCustomerCountByModule(module: String): Flow<Int>

    @Query("UPDATE customers SET isMuted = :isMuted WHERE id = :customerId")
    suspend fun updateMuteStatus(customerId: Long, isMuted: Boolean)

    @Query("UPDATE customers SET reservationDate = :date, reservationNote = :note WHERE id = :customerId")
    suspend fun updateReservation(customerId: Long, date: Long?, note: String?)

    @Query("SELECT * FROM customers WHERE reservationDate IS NOT NULL AND isArchived = 0 ORDER BY reservationDate ASC")
    fun getCustomersWithReservation(): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
