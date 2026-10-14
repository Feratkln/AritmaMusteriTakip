package com.aritma.musteritakip.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aritma.musteritakip.data.local.dao.CustomerDao
import com.aritma.musteritakip.data.local.dao.DistrictDao
import com.aritma.musteritakip.data.local.dao.NoteDao
import com.aritma.musteritakip.data.local.entity.CustomerEntity
import com.aritma.musteritakip.data.local.entity.DistrictEntity
import com.aritma.musteritakip.data.local.entity.NoteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import androidx.room.migration.Migration

/**
 * Uygulamanın yerel SQLite veri tabanı katmanı (Room Database).
 * Müşteri, not ve ilçe tablolarını yönetir; sürüm yükseltmelerinde
 * veri kaybını önleyen güvenli SQL göç (migration) adımlarını barındırır.
 */
@Database(
    entities = [CustomerEntity::class, NoteEntity::class, DistrictEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun noteDao(): NoteDao
    abstract fun districtDao(): DistrictDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE customers ADD COLUMN reservationDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE customers ADD COLUMN reservationNote TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN companyName TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN companyPhone TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aritma_musteri_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultDistricts(database.districtDao())
                    }
                }
            }

            private suspend fun populateDefaultDistricts(districtDao: DistrictDao) {
                val defaultDistricts = listOf(
                    "Arnavutköy",
                    "Avcılar",
                    "Bağcılar",
                    "Bahçelievler",
                    "Bakırköy",
                    "Başakşehir",
                    "Beylikdüzü",
                    "Büyükçekmece",
                    "Çatalca",
                    "Esenyurt",
                    "Eyüpsultan",
                    "Gaziosmanpaşa",
                    "Güngören",
                    "Küçükçekmece",
                    "Sarıyer",
                    "Silivri",
                    "Sultangazi",
                    "Şişli",
                    "Zeytinburnu",
                    "Kadıköy",
                    "Pendik",
                    "Ümraniye",
                    "Üsküdar"
                ).map { DistrictEntity(name = it) }

                districtDao.insertAll(defaultDistricts)
            }
        }
    }
}
