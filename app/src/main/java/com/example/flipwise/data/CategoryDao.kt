package com.example.flipwise.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Query("SELECT * FROM categories WHERE deckId = :deckId ORDER BY name ASC")
    suspend fun getForDeck(deckId: Int): List<Category>

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM categories WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)
}