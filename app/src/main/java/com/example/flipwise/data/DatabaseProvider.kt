package com.example.flipwise.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun get(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val i = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "flipwise_db"
            )
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .build()
            INSTANCE = i
            i
        }
    }
}