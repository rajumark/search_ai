package com.photo.searchai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.photo.searchai.core.database.dao.CleanupDao
import com.photo.searchai.core.database.dao.DuplicateDao
import com.photo.searchai.core.database.dao.ExifDao
import com.photo.searchai.core.database.dao.FaceDao
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.dao.ImageQualityDao
import com.photo.searchai.core.database.dao.OcrTextDao
import com.photo.searchai.core.database.dao.SmartAlbumDao
import com.photo.searchai.core.database.dao.VaultDao
import com.photo.searchai.core.database.dao.WorkerHistoryDao
import com.photo.searchai.core.database.entity.BarcodeEntity
import com.photo.searchai.core.database.entity.CleanupCandidateEntity
import com.photo.searchai.core.database.entity.DuplicateGroupEntity
import com.photo.searchai.core.database.entity.DuplicateMappingEntity
import com.photo.searchai.core.database.entity.ExifEntity
import com.photo.searchai.core.database.entity.FaceEntity
import com.photo.searchai.core.database.entity.ImageEntity
import com.photo.searchai.core.database.entity.ImageLabelEntity
import com.photo.searchai.core.database.entity.ImageQualityEntity
import com.photo.searchai.core.database.entity.OcrTextEntity
import com.photo.searchai.core.database.entity.SmartAlbumRuleEntity
import com.photo.searchai.core.database.entity.VaultEntity
import com.photo.searchai.core.database.entity.WorkerHistoryEntity

/**
 * Room database for the photo search system. Stores images, OCR text, barcodes, image labels,
 * faces, and worker history. Added support for metadata-driven organization in version 6.
 */
@Database(
        entities =
                [
                        ImageEntity::class,
                        OcrTextEntity::class,
                        BarcodeEntity::class,
                        ImageLabelEntity::class,
                        FaceEntity::class,
                        WorkerHistoryEntity::class,
                        ImageQualityEntity::class,
                        ExifEntity::class,
                        DuplicateGroupEntity::class,
                        DuplicateMappingEntity::class,
                        SmartAlbumRuleEntity::class,
                        CleanupCandidateEntity::class,
                        VaultEntity::class],
        version = 6,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
        abstract fun imageDao(): ImageDao
        abstract fun ocrTextDao(): OcrTextDao
        abstract fun barcodeDao(): BarcodeDao
        abstract fun imageLabelDao(): ImageLabelDao
        abstract fun faceDao(): FaceDao
        abstract fun imageQualityDao(): ImageQualityDao
        abstract fun workerHistoryDao(): WorkerHistoryDao
        abstract fun exifDao(): ExifDao
        abstract fun duplicateDao(): DuplicateDao
        abstract fun smartAlbumDao(): SmartAlbumDao
        abstract fun cleanupDao(): CleanupDao
        abstract fun vaultDao(): VaultDao

        companion object {
                const val DATABASE_NAME = "photo_search_db"

                /**
                 * Migration from version 1 to 2. Adds barcodeParsed, labelParsed columns to images
                 * table. Creates barcodes and image_labels tables.
                 */
                val MIGRATION_1_2 =
                        object : Migration(1, 2) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // Add new columns to images table
                                        db.execSQL(
                                                "ALTER TABLE images ADD COLUMN barcodeParsed INTEGER NOT NULL DEFAULT 0"
                                        )
                                        db.execSQL(
                                                "ALTER TABLE images ADD COLUMN labelParsed INTEGER NOT NULL DEFAULT 0"
                                        )

                                        // Create barcodes table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS barcodes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaStoreId INTEGER NOT NULL,
                        format INTEGER NOT NULL,
                        formatName TEXT NOT NULL,
                        rawValue TEXT NOT NULL,
                        displayValue TEXT NOT NULL,
                        valueType INTEGER NOT NULL,
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_barcodes_mediaStoreId ON barcodes(mediaStoreId)"
                                        )

                                        // Create image_labels table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS image_labels (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaStoreId INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        `index` INTEGER NOT NULL,
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_image_labels_mediaStoreId ON image_labels(mediaStoreId)"
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_image_labels_label ON image_labels(label)"
                                        )
                                }
                        }

                /**
                 * Migration from version 2 to 3. Creates worker_history table for tracking periodic
                 * background processing.
                 */
                val MIGRATION_2_3 =
                        object : Migration(2, 3) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS worker_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        durationMs INTEGER,
                        status TEXT NOT NULL DEFAULT 'RUNNING',
                        imagesProcessedOcr INTEGER NOT NULL DEFAULT 0,
                        imagesProcessedBarcode INTEGER NOT NULL DEFAULT 0,
                        imagesProcessedLabel INTEGER NOT NULL DEFAULT 0,
                        errorMessage TEXT,
                        isScheduledRun INTEGER NOT NULL DEFAULT 1
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_worker_history_startTime ON worker_history(startTime)"
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_worker_history_status ON worker_history(status)"
                                        )
                                }
                        }

                /**
                 * Migration from version 3 to 4. Adds faceParsed column to images table and creates
                 * faces table for storing detected face data.
                 */
                val MIGRATION_3_4 =
                        object : Migration(3, 4) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // Add faceParsed column to images table
                                        db.execSQL(
                                                "ALTER TABLE images ADD COLUMN faceParsed INTEGER NOT NULL DEFAULT 0"
                                        )

                                        // Create faces table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS faces (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaStoreId INTEGER NOT NULL,
                        croppedFacePath TEXT NOT NULL,
                        boundingBoxLeft INTEGER NOT NULL,
                        boundingBoxTop INTEGER NOT NULL,
                        boundingBoxRight INTEGER NOT NULL,
                        boundingBoxBottom INTEGER NOT NULL,
                        faceWidth INTEGER NOT NULL,
                        faceHeight INTEGER NOT NULL,
                        faceIndex INTEGER NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_faces_mediaStoreId ON faces(mediaStoreId)"
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_faces_croppedFacePath ON faces(croppedFacePath)"
                                        )
                                }
                        }

                /**
                 * Migration from version 4 to 5. Adds qualityParsed column to images table and
                 * creates image_quality table for storing quality metrics.
                 */
                val MIGRATION_4_5 =
                        object : Migration(4, 5) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // Add qualityParsed column to images table
                                        db.execSQL(
                                                "ALTER TABLE images ADD COLUMN qualityParsed INTEGER NOT NULL DEFAULT 0"
                                        )

                                        // Create image_quality table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS image_quality (
                        mediaStoreId INTEGER PRIMARY KEY NOT NULL,
                        blurScore REAL NOT NULL,
                        brightnessScore REAL NOT NULL,
                        contrastScore REAL NOT NULL,
                        overexposedRatio REAL NOT NULL,
                        width INTEGER NOT NULL,
                        height INTEGER NOT NULL,
                        imageHash TEXT NOT NULL,
                        analyzedAt INTEGER NOT NULL,
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_image_quality_mediaStoreId ON image_quality(mediaStoreId)"
                                        )
                                }
                        }

                /**
                 * Migration from version 5 to 6. Adds metadata-driven organization tables:
                 * exif_metadata, duplicate_groups, duplicate_mappings, smart_album_rules,
                 * cleanup_candidates, and vault_entries.
                 */
                val MIGRATION_5_6 =
                        object : Migration(5, 6) {
                                override fun migrate(db: SupportSQLiteDatabase) {
                                        // Add metadataParsed column to images table
                                        db.execSQL(
                                                "ALTER TABLE images ADD COLUMN metadataParsed INTEGER NOT NULL DEFAULT 0"
                                        )

                                        // Create exif_metadata table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS exif_metadata (
                        mediaStoreId INTEGER PRIMARY KEY NOT NULL,
                        make TEXT,
                        model TEXT,
                        flash INTEGER,
                        focalLength REAL,
                        iso INTEGER,
                        exposureTime REAL,
                        aperture REAL,
                        width INTEGER NOT NULL,
                        height INTEGER NOT NULL,
                        orientation INTEGER NOT NULL,
                        dateTaken INTEGER,
                        latitude REAL,
                        longitude REAL,
                        software TEXT,
                        isEdited INTEGER NOT NULL DEFAULT 0,
                        hasExif INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_exif_metadata_mediaStoreId ON exif_metadata(mediaStoreId)"
                                        )

                                        // Create duplicate_groups table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS duplicate_groups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        groupHash TEXT NOT NULL,
                        type TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_duplicate_groups_groupHash ON duplicate_groups(groupHash)"
                                        )

                                        // Create duplicate_mappings table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS duplicate_mappings (
                        mediaStoreId INTEGER NOT NULL,
                        groupId INTEGER NOT NULL,
                        isOriginal INTEGER NOT NULL DEFAULT 0,
                        score REAL NOT NULL DEFAULT 0,
                        PRIMARY KEY(mediaStoreId, groupId),
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE,
                        FOREIGN KEY(groupId) REFERENCES duplicate_groups(id) ON DELETE CASCADE
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_duplicate_mappings_groupId ON duplicate_mappings(groupId)"
                                        )

                                        // Create smart_album_rules table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS smart_album_rules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        ruleType TEXT NOT NULL,
                        configurationJson TEXT NOT NULL,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """
                                        )

                                        // Create cleanup_candidates table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS cleanup_candidates (
                        mediaStoreId INTEGER PRIMARY KEY NOT NULL,
                        reason TEXT NOT NULL,
                        reclaimableSize INTEGER NOT NULL,
                        suggestion TEXT NOT NULL,
                        identifiedAt INTEGER NOT NULL,
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_cleanup_candidates_mediaStoreId ON cleanup_candidates(mediaStoreId)"
                                        )

                                        // Create vault_entries table
                                        db.execSQL(
                                                """
                    CREATE TABLE IF NOT EXISTS vault_entries (
                        mediaStoreId INTEGER PRIMARY KEY NOT NULL,
                        originalPath TEXT NOT NULL,
                        vaultPath TEXT NOT NULL,
                        movedAt INTEGER NOT NULL,
                        isLocked INTEGER NOT NULL DEFAULT 1,
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE
                    )
                """
                                        )
                                        db.execSQL(
                                                "CREATE INDEX IF NOT EXISTS index_vault_entries_mediaStoreId ON vault_entries(mediaStoreId)"
                                        )
                                }
                        }
        }
}
