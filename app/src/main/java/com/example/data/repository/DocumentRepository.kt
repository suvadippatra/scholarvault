package com.scholarvault.data.repository

import com.scholarvault.data.dao.DocumentDao
import com.scholarvault.data.dao.WalletDao
import com.scholarvault.data.model.*
import kotlinx.coroutines.flow.Flow

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val walletDao: WalletDao
) {
    // --- Documents ---
    fun getRootFiles(): Flow<List<DocumentFile>> = documentDao.getRootFiles()
    
    fun getAllFolders(): Flow<List<DocumentFile>> = documentDao.getAllFolders()
    
    fun getFilesByFolder(folderId: Int): Flow<List<DocumentFile>> = documentDao.getFilesByFolder(folderId)
    
    suspend fun getFilesByFolderForDeletion(folderId: Int): List<DocumentFile> = documentDao.getFilesByFolderForDeletion(folderId)
    
    fun getTrashedFiles(): Flow<List<DocumentFile>> = documentDao.getTrashedFiles()
    
    fun getAllNonFolderFiles(): Flow<List<DocumentFile>> = documentDao.getAllNonFolderFiles()
    
    suspend fun getFileById(id: Int): DocumentFile? = documentDao.getFileById(id)
    
    suspend fun getFileByFilePath(filePath: String): DocumentFile? = documentDao.getFileByFilePath(filePath)
    
    suspend fun getFileByName(name: String): DocumentFile? = documentDao.getFileByName(name)
    
    fun searchFiles(query: String): Flow<List<DocumentFile>> = documentDao.searchFiles(query)
    
    suspend fun insertFile(documentFile: DocumentFile): Long = documentDao.insertFile(documentFile)
    
    suspend fun updateFile(documentFile: DocumentFile) = documentDao.updateFile(documentFile)
    
    suspend fun deleteFile(documentFile: DocumentFile) = documentDao.deleteFile(documentFile)
    
    suspend fun moveToTrash(id: Int) = documentDao.moveToTrash(id)
    
    suspend fun restoreFromTrash(id: Int) = documentDao.restoreFromTrash(id)

    // --- Wallet ---
    fun getAllCategories(): Flow<List<WalletCategory>> = walletDao.getAllCategories()
    
    fun getAllCards(): Flow<List<WalletCard>> = walletDao.getAllCards()
    
    suspend fun insertCategory(category: WalletCategory): Long = walletDao.insertCategory(category)
    
    suspend fun updateCategory(category: WalletCategory) = walletDao.updateCategory(category)
    
    suspend fun deleteCategory(category: WalletCategory) = walletDao.deleteCategory(category)
    
    fun getCardsByCategory(categoryId: Int): Flow<List<WalletCard>> = walletDao.getCardsByCategory(categoryId)
    
    suspend fun insertCardWithFields(card: WalletCard, fields: List<WalletCardCustomField>) {
        // Convert fields to JSON
        val jsonArray = org.json.JSONArray()
        fields.forEach { field ->
            val obj = org.json.JSONObject()
            obj.put("label", field.label)
            obj.put("value", field.value)
            obj.put("orderIndex", field.orderIndex)
            jsonArray.put(obj)
        }
        
        val updatedCard = card.copy(customFieldsJson = jsonArray.toString())
        walletDao.insertCard(updatedCard)
    }

    suspend fun updateCardWithFields(card: WalletCard, fields: List<WalletCardCustomField>) {
        val jsonArray = org.json.JSONArray()
        fields.forEach { field ->
            val obj = org.json.JSONObject()
            obj.put("label", field.label)
            obj.put("value", field.value)
            obj.put("orderIndex", field.orderIndex)
            jsonArray.put(obj)
        }
        
        val updatedCard = card.copy(customFieldsJson = jsonArray.toString())
        walletDao.updateCard(updatedCard)
    }

    suspend fun deleteCard(card: WalletCard) = walletDao.deleteCard(card)
}
