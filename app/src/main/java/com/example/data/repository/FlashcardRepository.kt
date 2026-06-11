package com.scholarvault.data.repository

import com.scholarvault.data.dao.FlashcardDao
import com.scholarvault.data.model.FlashcardDeckEntity
import com.scholarvault.data.model.FlashcardEntity
import kotlinx.coroutines.flow.Flow

class FlashcardRepository(private val flashcardDao: FlashcardDao) {

    fun getAllDecks(): Flow<List<FlashcardDeckEntity>> = flashcardDao.getAllDecks()

    suspend fun getDeckById(deckId: String): FlashcardDeckEntity? = flashcardDao.getDeckById(deckId)

    suspend fun insertDeck(deck: FlashcardDeckEntity) = flashcardDao.insertDeck(deck)

    suspend fun deleteDeck(deck: FlashcardDeckEntity) {
        flashcardDao.deleteCardsByDeckId(deck.id)
        flashcardDao.deleteDeck(deck)
    }

    fun getCardsForDeck(deckId: String): Flow<List<FlashcardEntity>> = flashcardDao.getCardsForDeck(deckId)

    suspend fun insertCard(card: FlashcardEntity) = flashcardDao.insertCard(card)

    suspend fun deleteCard(card: FlashcardEntity) = flashcardDao.deleteCard(card)
}
