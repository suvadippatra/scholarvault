package com.scholarvault.data.dao

import androidx.room.*
import com.scholarvault.data.model.ScannedDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedDocumentDao {
    @Query("SELECT * FROM scanned_documents WHERE isTrashed = 0 ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScannedDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScannedDocumentEntity)

    @Delete
    suspend fun deleteScan(scan: ScannedDocumentEntity)
}
