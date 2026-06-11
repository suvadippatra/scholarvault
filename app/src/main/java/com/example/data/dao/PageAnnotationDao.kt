package com.scholarvault.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.scholarvault.data.model.PageAnnotation
import kotlinx.coroutines.flow.Flow

@Dao
interface PageAnnotationDao {
    @Query("SELECT * FROM page_annotations WHERE documentId = :documentId AND pageIndex = :pageIndex")
    fun getAnnotationsForPage(documentId: Int, pageIndex: Int): Flow<List<PageAnnotation>>

    @Query("SELECT * FROM page_annotations WHERE documentId = :documentId")
    fun getAllAnnotationsForDocument(documentId: Int): Flow<List<PageAnnotation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: PageAnnotation): Long

    @Update
    suspend fun updateAnnotation(annotation: PageAnnotation)

    @Delete
    suspend fun deleteAnnotation(annotation: PageAnnotation)

    @Query("DELETE FROM page_annotations WHERE documentId = :documentId AND pageIndex = :pageIndex")
    suspend fun clearAnnotationsForPage(documentId: Int, pageIndex: Int)
}
