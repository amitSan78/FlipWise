package com.example.flipwise.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: Word)

    @Query("SELECT * FROM words WHERE categoryId = :categoryId ORDER BY id ASC")
    suspend fun getForCategory(categoryId: Int): List<Word>
}
