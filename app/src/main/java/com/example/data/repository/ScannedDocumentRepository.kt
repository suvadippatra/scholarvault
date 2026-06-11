package com.scholarvault.data.repository

import com.scholarvault.data.dao.ScannedDocumentDao
import com.scholarvault.data.model.ScannedDocumentEntity
import kotlinx.coroutines.flow.Flow

class ScannedDocumentRepository(private val dao: ScannedDocumentDao) {
    fun getAllScans(): Flow<List<ScannedDocumentEntity>> = dao.getAllScans()

    suspend fun insertScan(scan: ScannedDocumentEntity) {
        dao.insertScan(scan)
    }

    suspend fun deleteScan(scan: ScannedDocumentEntity) {
        dao.deleteScan(scan)
    }
}
