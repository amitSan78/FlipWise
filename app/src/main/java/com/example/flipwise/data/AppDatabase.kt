package com.example.flipwise.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Deck::class, Category::class, Word::class],
    version = 2,  // Increment version for update methods
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun categoryDao(): CategoryDao
    abstract fun wordDao(): WordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes needed, just added @Update methods to DAOs
                // This migration is just to bump the version number
            }
        }
    }
}