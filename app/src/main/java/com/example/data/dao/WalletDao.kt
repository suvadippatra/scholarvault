package com.scholarvault.data.dao

import androidx.room.*
import com.scholarvault.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {

    // --- Categories ---
    @Query("SELECT * FROM wallet_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<WalletCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: WalletCategory): Long

    @Update
    suspend fun updateCategory(category: WalletCategory)

    @Delete
    suspend fun deleteCategory(category: WalletCategory)

    // --- Cards ---
    @Query("SELECT * FROM wallet_cards ORDER BY id ASC")
    fun getAllCards(): Flow<List<WalletCard>>

    @Query("SELECT * FROM wallet_cards WHERE title LIKE '%' || :searchQuery || '%' ORDER BY id ASC")
    fun searchCards(searchQuery: String): Flow<List<WalletCard>>

    @Query("SELECT * FROM wallet_cards WHERE categoryId = :categoryId")
    fun getCardsByCategory(categoryId: Int): Flow<List<WalletCard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: WalletCard): Long

    @Update
    suspend fun updateCard(card: WalletCard)

    @Delete
    suspend fun deleteCard(card: WalletCard)
}
