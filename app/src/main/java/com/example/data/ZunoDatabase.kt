package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SongProject::class], version = 2, exportSchema = false)
abstract class ZunoDatabase : RoomDatabase() {
    abstract fun songProjectDao(): SongProjectDao

    companion object {
        @Volatile
        private var INSTANCE: ZunoDatabase? = null

        fun getInstance(context: Context): ZunoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZunoDatabase::class.java,
                    "zuno_56300.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
