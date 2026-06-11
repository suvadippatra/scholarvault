package com.scholarvault.data.dao

import androidx.room.*
import com.scholarvault.data.model.TransactionAccount
import com.scholarvault.data.model.TransactionItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionLedgerDao {
    @Query("SELECT * FROM transaction_accounts")
    fun getAllAccounts(): Flow<List<TransactionAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: TransactionAccount): Long

    @Update
    suspend fun updateAccount(account: TransactionAccount)

    @Delete
    suspend fun deleteAccount(account: TransactionAccount)

    @Query("SELECT * FROM transaction_items WHERE accountId = :accountId ORDER BY timestamp DESC")
    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionItem>>

    @Query("SELECT * FROM transaction_items ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionItem): Long
    
    @Update
    suspend fun updateTransaction(transaction: TransactionItem)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionItem)

    @Transaction // Run in transaction to update balance reliably
    suspend fun insertTransactionAndUpdateBalance(item: TransactionItem, account: TransactionAccount) {
        insertTransaction(item)
        val change = if (item.isExpense) -item.amount else item.amount
        val updatedAccount = account.copy(currentBalance = account.currentBalance + change)
        updateAccount(updatedAccount)
    }

    @Query("SELECT * FROM transaction_accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: Int): TransactionAccount?
}
