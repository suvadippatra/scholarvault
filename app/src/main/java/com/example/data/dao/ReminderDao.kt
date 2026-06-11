package com.scholarvault.data.dao

import androidx.room.*
import com.scholarvault.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isDeleted = 0 ORDER BY timeInMillis ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isDeleted = 0 ORDER BY timeInMillis ASC")
    suspend fun getAllRemindersOnce(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isDeleted = 1 ORDER BY timeInMillis ASC")
    fun getDeletedReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isDeleted = 0 AND title LIKE '%' || :query || '%' ORDER BY timeInMillis ASC")
    fun searchReminders(query: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    // Soft delete
    @Query("UPDATE reminders SET isDeleted = 1 WHERE id IN (:ids)")
    suspend fun softDeleteReminders(ids: Set<String>)

    // Hard delete
    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun hardDeleteReminders(ids: Set<String>)

    // Restore
    @Query("UPDATE reminders SET isDeleted = 0 WHERE id IN (:ids)")
    suspend fun restoreReminders(ids: Set<String>)
}
