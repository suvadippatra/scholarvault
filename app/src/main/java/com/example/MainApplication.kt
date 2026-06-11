package com.scholarvault

// Re-forcing compile!
import android.app.Application

// Cache busted!
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.scholarvault.data.AppDatabase

class MainApplication : Application() {
    lateinit var database: AppDatabase
        private set

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Semesters table updates
            database.execSQL("ALTER TABLE semesters ADD COLUMN semester_label TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE semesters ADD COLUMN subjects_json TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE semesters ADD COLUMN total_credits REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE semesters ADD COLUMN earned_sgpa REAL")

            // AcademicItem table updates
            database.execSQL("ALTER TABLE academic_items ADD COLUMN board_roll_number TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN board_roll_code TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN board_registration_number TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN marksheet_index_number TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN school_code TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN medium_of_instruction TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN stream TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN use_best_of_five INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN passing_status TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN nta_percentile REAL")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN all_india_rank INTEGER")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN category_rank INTEGER")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN domicile_state TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN institution_district TEXT")
        }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE semesters ADD COLUMN roll_number TEXT")
            database.execSQL("ALTER TABLE semesters ADD COLUMN override_roll_number INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE semesters ADD COLUMN attached_document_id TEXT")
            database.execSQL("ALTER TABLE semesters ADD COLUMN attached_document_name TEXT")

            database.execSQL("ALTER TABLE academic_items ADD COLUMN grade_conversion_formula TEXT")
            database.execSQL("ALTER TABLE academic_items ADD COLUMN best_of_n INTEGER")
        }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `page_annotations` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `documentId` INTEGER NOT NULL, 
                    `pageIndex` INTEGER NOT NULL, 
                    `typedNote` TEXT, 
                    `handwrittenDrawingJson` TEXT, 
                    `lastModified` INTEGER NOT NULL, 
                    FOREIGN KEY(`documentId`) REFERENCES `document_files`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_page_annotations_documentId` ON `page_annotations` (`documentId`)")
        }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `quick_notes` (
                    `id` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `colorHex` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `flashcard_decks` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `flashcards` (
                    `id` TEXT NOT NULL,
                    `deckId` TEXT NOT NULL,
                    `frontText` TEXT NOT NULL,
                    `backText` TEXT NOT NULL,
                    `masteryLevel` INTEGER NOT NULL,
                    `lastReviewed` INTEGER,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
        }
    }

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE quick_notes ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'")
            database.execSQL("ALTER TABLE quick_notes ADD COLUMN folder TEXT")
        }
    }

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER")
        }
    }

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `scanned_documents` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `pagePaths` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `isTrashed` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }
    }

    val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE scanned_documents ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        }
    }

    val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `transaction_accounts` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `initialBalance` REAL NOT NULL, 
                    `currentBalance` REAL NOT NULL
                )
            """.trimIndent())
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `transaction_items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `accountId` INTEGER NOT NULL, 
                    `timestamp` INTEGER NOT NULL, 
                    `category` TEXT NOT NULL, 
                    `details` TEXT NOT NULL, 
                    `amount` REAL NOT NULL, 
                    `isExpense` INTEGER NOT NULL, 
                    `attachmentDocId` TEXT, 
                    `meterReadingStart` REAL, 
                    `meterReadingEnd` REAL, 
                    `meterRatesJson` TEXT, 
                    FOREIGN KEY(`accountId`) REFERENCES `transaction_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_items_accountId` ON `transaction_items` (`accountId`)")
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java, "scholar-vault-db"
        )
        .addMigrations(MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
        .fallbackToDestructiveMigration(false)
        .build()

        // Asynchronously sweep decrypt file cache older than 1 hour
        Thread {
            try {
                val oneHourAgo = System.currentTimeMillis() - 3600000L
                val sharedDir = java.io.File(cacheDir, "shared")
                if (sharedDir.exists() && sharedDir.isDirectory) {
                    sharedDir.listFiles()?.forEach { file ->
                        if (file.lastModified() < oneHourAgo) {
                            file.delete()
                        }
                    }
                }
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.lastModified() < oneHourAgo) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
