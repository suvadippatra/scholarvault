package com.scholarvault.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.scholarvault.data.dao.*
import com.scholarvault.data.model.*

@Database(
    entities = [
        AcademicItem::class,
        Semester::class,
        AcademicDocumentLink::class,
        Transaction::class,
        UserProfile::class,
        ProfileExperience::class,
        ProfileWork::class,
        DocumentFile::class,
        WalletCategory::class,
        WalletCard::class,
        TrashEntity::class,
        WalletAttachmentEntity::class,
        FolderEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        PageAnnotation::class,
        QuickNoteEntity::class,
        FlashcardDeckEntity::class,
        FlashcardEntity::class,
        ScannedDocumentEntity::class,
        TransactionAccount::class,
        TransactionItem::class
    ],
    version = 19,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun academicItemDao(): AcademicItemDao
    abstract fun semesterDao(): SemesterDao
    abstract fun academicDocumentLinkDao(): AcademicDocumentLinkDao
    abstract fun transactionDao(): TransactionDao
    abstract fun profileDao(): ProfileDao
    abstract fun documentDao(): DocumentDao
    abstract fun walletDao(): WalletDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun pageAnnotationDao(): PageAnnotationDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun scannedDocumentDao(): ScannedDocumentDao
    abstract fun transactionLedgerDao(): TransactionLedgerDao
}
