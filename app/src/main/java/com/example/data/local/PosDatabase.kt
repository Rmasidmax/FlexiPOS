package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [AppSettings::class, Product::class, UtangRecord::class, ReceiptLog::class],
    version = 2,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun posDao(): PosDao

    companion object {
        @Volatile
        private var INSTANCE: PosDatabase? = null

        const val DATABASE_NAME = "pos_database.db"

        fun getDatabase(context: Context): PosDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PosDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
