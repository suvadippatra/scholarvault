package com.scholarvault.data.dao

import androidx.room.*
import com.scholarvault.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY rowid ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY rowid ASC")
    fun getDeletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND title LIKE '%' || :query || '%' ORDER BY rowid ASC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    // Soft delete
    @Query("UPDATE tasks SET isDeleted = 1 WHERE id IN (:ids)")
    suspend fun softDeleteTasks(ids: Set<String>)

    // Hard delete (from trash)
    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun hardDeleteTasks(ids: Set<String>)

    @Query("UPDATE tasks SET isDeleted = 0 WHERE id IN (:ids)")
    suspend fun restoreTasks(ids: Set<String>)
}
