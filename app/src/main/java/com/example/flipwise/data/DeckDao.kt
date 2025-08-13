package com.example.flipwise.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deck: Deck)

    @Query("SELECT * FROM decks ORDER BY name ASC")
    suspend fun getAllDecks(): List<Deck>
}
