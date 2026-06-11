package com.scholarvault.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.scholarvault.data.model.AcademicDocumentLink
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicDocumentLinkDao {
    @Query("SELECT * FROM academic_document_links WHERE academic_item_id = :academicItemId")
    fun getLinksForAcademicItem(academicItemId: String): Flow<List<AcademicDocumentLink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: AcademicDocumentLink)

    @Delete
    suspend fun delete(link: AcademicDocumentLink)
}
