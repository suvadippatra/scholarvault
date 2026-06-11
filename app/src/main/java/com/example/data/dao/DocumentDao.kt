package com.scholarvault.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.scholarvault.data.model.DocumentFile
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM document_files WHERE parentFolderId IS NULL AND isTrashed = 0 ORDER BY isFolder DESC, name ASC")
    fun getRootFiles(): Flow<List<DocumentFile>>

    @Query("SELECT * FROM document_files WHERE isFolder = 1 AND isTrashed = 0 ORDER BY name ASC")
    fun getAllFolders(): Flow<List<DocumentFile>>

    @Query("SELECT * FROM document_files WHERE parentFolderId = :folderId AND isTrashed = 0 ORDER BY isFolder DESC, name ASC")
    fun getFilesByFolder(folderId: Int): Flow<List<DocumentFile>>

    @Query("SELECT * FROM document_files WHERE parentFolderId = :folderId")
    suspend fun getFilesByFolderForDeletion(folderId: Int): List<DocumentFile>

    @Query("SELECT * FROM document_files WHERE isTrashed = 1 ORDER BY name ASC")
    fun getTrashedFiles(): Flow<List<DocumentFile>>

    @Query("SELECT * FROM document_files WHERE isFolder = 0 AND isTrashed = 0")
    fun getAllNonFolderFiles(): Flow<List<DocumentFile>>

    @Query("SELECT * FROM document_files WHERE id = :id")
    suspend fun getFileById(id: Int): DocumentFile?

    @Query("SELECT * FROM document_files WHERE name LIKE '%' || :searchQuery || '%' AND isTrashed = 0")
    fun searchFiles(searchQuery: String): Flow<List<DocumentFile>>

    @Query("SELECT * FROM document_files WHERE filePath = :filePath")
    suspend fun getFileByFilePath(filePath: String): DocumentFile?

    @Query("SELECT * FROM document_files WHERE name = :name AND isFolder = 0 LIMIT 1")
    suspend fun getFileByName(name: String): DocumentFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(documentFile: DocumentFile): Long

    @Update
    suspend fun updateFile(documentFile: DocumentFile)

    @Delete
    suspend fun deleteFile(documentFile: DocumentFile)

    @Query("UPDATE document_files SET isTrashed = 1 WHERE id = :id")
    suspend fun moveToTrash(id: Int)

    @Query("UPDATE document_files SET isTrashed = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Int)
}
