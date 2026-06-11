package com.scholarvault.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "academic_document_links",
    primaryKeys = ["academic_item_id", "wallet_document_id"],
    foreignKeys = [
        ForeignKey(
            entity = AcademicItem::class,
            parentColumns = ["id"],
            childColumns = ["academic_item_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("academic_item_id"),
        Index("wallet_document_id")
    ]
)
data class AcademicDocumentLink(
    @ColumnInfo(name = "academic_item_id") val academicItemId: String,
    @ColumnInfo(name = "wallet_document_id") val walletDocumentId: String,
    @ColumnInfo(name = "link_label") val linkLabel: String
)
