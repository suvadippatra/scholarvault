package com.scholarvault.data.repository

import com.scholarvault.data.dao.QuickNoteDao
import com.scholarvault.data.model.QuickNoteEntity
import kotlinx.coroutines.flow.Flow

class QuickNoteRepository(private val quickNoteDao: QuickNoteDao) {

    fun getAllQuickNotes(): Flow<List<QuickNoteEntity>> = quickNoteDao.getAllQuickNotes()

    suspend fun insertQuickNote(note: QuickNoteEntity) {
        quickNoteDao.insertQuickNote(note)
    }

    suspend fun deleteQuickNote(note: QuickNoteEntity) {
        quickNoteDao.deleteQuickNote(note)
    }
}
