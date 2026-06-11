package com.scholarvault.data.repository

import com.scholarvault.data.dao.ReminderDao
import com.scholarvault.data.model.ReminderEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ReminderRepository(private val dao: ReminderDao) {

    val allReminders: Flow<List<ReminderEntity>> = dao.getAllReminders()
    val deletedReminders: Flow<List<ReminderEntity>> = dao.getDeletedReminders()

    suspend fun addReminder(title: String, dateTimeStr: String, timeInMillis: Long) {
        dao.insertReminder(
            ReminderEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                dateTimeStr = dateTimeStr,
                timeInMillis = timeInMillis
            )
        )
    }

    suspend fun softDeleteReminders(ids: Set<String>) = dao.softDeleteReminders(ids)
    suspend fun hardDeleteReminders(ids: Set<String>) = dao.hardDeleteReminders(ids)
    suspend fun restoreReminders(ids: Set<String>) = dao.restoreReminders(ids)
}
