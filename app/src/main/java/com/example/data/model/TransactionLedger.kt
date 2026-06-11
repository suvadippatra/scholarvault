package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "transaction_accounts")
data class TransactionAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val initialBalance: Double,
    val currentBalance: Double
)

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId")
    ]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val timestamp: Long,
    val category: String,
    val details: String,
    val amount: Double,
    val isExpense: Boolean,
    val attachmentDocId: String?, // ID to a document file, nullable
    val meterReadingStart: Double?,
    val meterReadingEnd: Double?,
    val meterRatesJson: String? // JSON string representing the tier rates
)
