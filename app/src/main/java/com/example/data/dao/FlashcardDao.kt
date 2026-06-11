package com.scholarvault.data.dao

import androidx.room.*
import com.scholarvault.data.model.FlashcardDeckEntity
import com.scholarvault.data.model.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcard_decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<FlashcardDeckEntity>>

    @Query("SELECT * FROM flashcard_decks WHERE id = :id")
    suspend fun getDeckById(id: String): FlashcardDeckEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: FlashcardDeckEntity)

    @Delete
    suspend fun deleteDeck(deck: FlashcardDeckEntity)

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId")
    fun getCardsForDeck(deckId: String): Flow<List<FlashcardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: FlashcardEntity)

    @Delete
    suspend fun deleteCard(card: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteCardsByDeckId(deckId: String)
}
