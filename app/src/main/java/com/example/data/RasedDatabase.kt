package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StolenVehicleEntity::class, SightingAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RasedDatabase : RoomDatabase() {
    abstract fun rasedDao(): RasedDao

    companion object {
        @Volatile
        private var INSTANCE: RasedDatabase? = null

        fun getInstance(context: Context): RasedDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RasedDatabase::class.java,
                    "rased_stolen_vehicles.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
