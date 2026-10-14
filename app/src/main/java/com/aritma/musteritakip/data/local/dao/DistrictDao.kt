package com.aritma.musteritakip.data.local.dao

import androidx.room.*
import com.aritma.musteritakip.data.local.entity.DistrictEntity
import kotlinx.coroutines.flow.Flow

/**
 * İlçe ve bölge tanımlarına yönelik veritabanı işlemlerini yürüten DAO arayüzü.
 * Filtreleme listeleri için ilçe kayıtlarını ve sıralı akışları yönetir.
 */
@Dao
interface DistrictDao {

    @Query("SELECT * FROM districts ORDER BY name ASC")
    fun getAllDistricts(): Flow<List<DistrictEntity>>

    @Query("SELECT * FROM districts ORDER BY name ASC")
    suspend fun getAllDistrictsList(): List<DistrictEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDistrict(district: DistrictEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(districts: List<DistrictEntity>)

    @Query("DELETE FROM districts")
    suspend fun deleteAll()
}
