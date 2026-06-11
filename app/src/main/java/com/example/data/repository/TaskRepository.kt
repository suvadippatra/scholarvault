package com.scholarvault.data.repository

import com.scholarvault.data.dao.TaskDao
import com.scholarvault.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TaskRepository(private val dao: TaskDao) {

    val activeTasks: Flow<List<TaskEntity>> = dao.getAllTasks()
    val deletedTasks: Flow<List<TaskEntity>> = dao.getDeletedTasks()

    suspend fun addTask(title: String) {
        dao.insertTask(TaskEntity(id = UUID.randomUUID().toString(), title = title))
    }

    suspend fun toggleTask(task: TaskEntity) {
        val newCompleted = !task.completed
        val newDate = if (newCompleted) System.currentTimeMillis() else null
        dao.updateTask(task.copy(completed = newCompleted, completedAt = newDate))
    }

    suspend fun softDeleteTasks(ids: Set<String>) = dao.softDeleteTasks(ids)
    suspend fun hardDeleteTasks(ids: Set<String>) = dao.hardDeleteTasks(ids)
    suspend fun restoreTasks(ids: Set<String>) = dao.restoreTasks(ids)
}
