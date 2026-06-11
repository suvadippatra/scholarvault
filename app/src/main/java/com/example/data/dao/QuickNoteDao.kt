package com.scholarvault.data.dao

import androidx.room.*
import com.scholarvault.data.model.QuickNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickNoteDao {
    @Query("SELECT * FROM quick_notes ORDER BY timestamp DESC")
    fun getAllQuickNotes(): Flow<List<QuickNoteEntity>>

    @Query("SELECT * FROM quick_notes WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchNotes(query: String): Flow<List<QuickNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickNote(note: QuickNoteEntity)

    @Delete
    suspend fun deleteQuickNote(note: QuickNoteEntity)
}
