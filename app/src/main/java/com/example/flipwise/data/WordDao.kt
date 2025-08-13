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

    @Query("SELECT * FROM words WHERE categoryId IN (:categoryIds) ORDER BY id ASC")
    suspend fun getForCategories(categoryIds: List<Int>): List<Word>

    @Query("DELETE FROM words WHERE id = :id")
    suspend fun deleteById(id: Int)


}
