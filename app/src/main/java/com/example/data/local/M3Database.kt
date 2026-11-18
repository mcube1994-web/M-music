package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [VaultTrackEntity::class, SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class M3Database : RoomDatabase() {
    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var instance: M3Database? = null

        fun getInstance(context: Context): M3Database {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    M3Database::class.java,
                    "m3_offline_vault.db"
                ).build().also { instance = it }
            }
        }
    }
}
