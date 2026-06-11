package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "flashcard_decks")
data class FlashcardDeckEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val createdAt: Date = Date()
)

@Entity(tableName = "flashcards")
data class FlashcardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val deckId: String,
    val frontText: String,
    val backText: String,
    val masteryLevel: Int = 0, // 0 = New/Learning, 1 = Familiar, 2 = Mastered
    val lastReviewed: Date? = null
)
