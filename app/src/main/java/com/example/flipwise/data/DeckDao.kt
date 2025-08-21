package com.example.flipwise.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deck: Deck)

    @Update
    suspend fun update(deck: Deck)

    @Query("SELECT * FROM decks ORDER BY name ASC")
    suspend fun getAllDecks(): List<Deck>

    @Query("SELECT * FROM decks ORDER BY id ASC")
    suspend fun getAll(): List<Deck>

    @Query("DELETE FROM decks WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM decks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)
}