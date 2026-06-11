package com.scholarvault.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String,
    val amount: Int,
    val category: String?,
    val notes: String?,
    @ColumnInfo(name = "transaction_date") val transactionDate: Date,
    @ColumnInfo(name = "document_path") val documentPath: String?,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date()
)
