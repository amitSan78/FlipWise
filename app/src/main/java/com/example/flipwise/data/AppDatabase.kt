package com.example.flipwise.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Deck::class, Category::class, Word::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun categoryDao(): CategoryDao
    abstract fun wordDao(): WordDao
}
