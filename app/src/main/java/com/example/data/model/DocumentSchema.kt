package com.scholarvault.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Relation
import androidx.room.Embedded
import java.util.Date

@Entity(
    tableName = "document_files",
    foreignKeys = [
        ForeignKey(
            entity = DocumentFile::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentFolderId"])]
)
data class DocumentFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isFolder: Boolean,
    val parentFolderId: Int? = null,
    val extension: String? = null,
    val sizeBytes: Long = 0,
    val createdAt: Date = Date(),
    val filePath: String, 
    val isEncrypted: Boolean = false,
    val tags: List<String> = emptyList(),
    val isTrashed: Boolean = false
)

@Entity(tableName = "wallet_categories")
data class WalletCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, 
    val iconName: String, 
    val isProtected: Boolean = true
)

@Entity(
    tableName = "wallet_cards",
    foreignKeys = [
        ForeignKey(
            entity = WalletCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocumentFile::class,
            parentColumns = ["id"],
            childColumns = ["linkedDocumentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["linkedDocumentId"])
    ]
)
data class WalletCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val title: String,
    val frontImagePath: String? = null,
    val backImagePath: String? = null,
    val notes: String? = null,
    val linkedDocumentId: Int? = null,
    val customFieldsJson: String? = null,
    val createdAt: Date = Date()
)

@Entity(tableName = "trash_entities")
data class TrashEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val deletedAt: Date = Date(),
    val originalParentId: Int? = null
)

@Entity(
    tableName = "wallet_attachments",
    foreignKeys = [
        ForeignKey(
            entity = WalletCard::class,
            parentColumns = ["id"],
            childColumns = ["walletCardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DocumentFile::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["walletCardId"]),
        Index(value = ["documentId"])
    ]
)
data class WalletAttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val walletCardId: Int,
    val documentId: Int
)

@Entity(
    tableName = "folder_entities",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentFolderId"])]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val parentFolderId: Int? = null,
    val createdAt: Date = Date()
)

data class WalletCardCustomField(
    val label: String,
    val value: String,
    val orderIndex: Int = 0
)

