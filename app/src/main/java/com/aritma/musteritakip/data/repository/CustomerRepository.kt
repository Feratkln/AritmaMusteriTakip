package com.aritma.musteritakip.data.repository

import com.aritma.musteritakip.data.local.dao.CustomerDao
import com.aritma.musteritakip.data.local.dao.DistrictDao
import com.aritma.musteritakip.data.local.dao.NoteDao
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.data.local.entity.DistrictEntity
import com.aritma.musteritakip.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Veri tabanı erişimini soyutlayan merkezi veri deposu (Repository).
 * Müşteri, not ve ilçe DAO işlemlerini tek bir arayüz altında toplayarak
 * ViewModel katmanına reaktif Flow akışları ve askıya alınabilir (suspend) fonksiyonlar sunar.
 */
class CustomerRepository(
    private val customerDao: CustomerDao,
    private val noteDao: NoteDao,
    private val districtDao: DistrictDao
) {

    fun getCustomersByModule(module: String): Flow<List<CustomerEntity>> =
        customerDao.getCustomersByModule(module)

    fun getCustomerByIdFlow(id: Long): Flow<CustomerEntity?> =
        customerDao.getCustomerByIdFlow(id)

    suspend fun getCustomerById(id: Long): CustomerEntity? =
        customerDao.getCustomerById(id)

    suspend fun insertCustomer(customer: CustomerEntity): Long =
        customerDao.insertCustomer(customer)

    suspend fun updateCustomer(customer: CustomerEntity) =
        customerDao.updateCustomer(customer)

    suspend fun deleteCustomer(customer: CustomerEntity) =
        customerDao.deleteCustomer(customer)

    suspend fun archiveCustomer(customerId: Long) =
        customerDao.archiveCustomer(customerId)

    suspend fun updateMaintenanceDate(customerId: Long, newDate: Long) {
        customerDao.updateMaintenanceDate(customerId, newDate)
        // Clear reservation if maintenance is completed
        customerDao.updateReservation(customerId, null, null)
    }

    suspend fun updateMuteStatus(customerId: Long, isMuted: Boolean) =
        customerDao.updateMuteStatus(customerId, isMuted)

    suspend fun updateReservation(customerId: Long, date: Long?, note: String?) =
        customerDao.updateReservation(customerId, date, note)

    fun getCustomersWithReservation(): Flow<List<CustomerEntity>> =
        customerDao.getCustomersWithReservation()

    fun getNotesForCustomer(customerId: Long): Flow<List<NoteEntity>> =
        noteDao.getNotesForCustomer(customerId)

    suspend fun addNote(customerId: Long, noteText: String): Long {
        val note = NoteEntity(customerId = customerId, noteText = noteText)
        return noteDao.insertNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) =
        noteDao.deleteNote(note)

    fun getAllDistricts(): Flow<List<DistrictEntity>> =
        districtDao.getAllDistricts()

    suspend fun addDistrict(name: String): Long {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return -1
        return districtDao.insertDistrict(DistrictEntity(name = cleanName))
    }

    fun getCustomerCountByModule(module: String): Flow<Int> =
        customerDao.getCustomerCountByModule(module)

    suspend fun getAllActiveCustomersList(): List<CustomerEntity> =
        customerDao.getAllActiveCustomersList()

    fun getAllActiveCustomersFlow(): Flow<List<CustomerEntity>> =
        customerDao.getAllActiveCustomersFlow()

    suspend fun getAllNotesList(): List<NoteEntity> =
        noteDao.getAllNotes()

    suspend fun restoreDatabase(customers: List<CustomerEntity>, notes: List<NoteEntity>) {
        customerDao.deleteAll()
        noteDao.deleteAll()
        customerDao.insertAll(customers)
        noteDao.insertAll(notes)
    }
}
