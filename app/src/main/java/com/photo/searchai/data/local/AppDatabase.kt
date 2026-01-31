package com.photo.searchai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.photo.searchai.data.local.dao.BarcodeDao
import com.photo.searchai.data.local.dao.FaceDao
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.ImageLabelDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.dao.WorkerHistoryDao
import com.photo.searchai.data.local.entity.BarcodeEntity
import com.photo.searchai.data.local.entity.FaceEntity
import com.photo.searchai.data.local.entity.ImageEntity
import com.photo.searchai.data.local.entity.ImageLabelEntity
import com.photo.searchai.data.local.entity.OcrTextEntity
import com.photo.searchai.data.local.entity.WorkerHistoryEntity

/**
 * Room database for the photo search system. Stores images, OCR text, barcodes, image labels,
 * faces, and worker history.
 */
@Database(
        entities =
                [
                        ImageEntity::class,
                        OcrTextEntity::class,
                        BarcodeEntity::class,
                        ImageLabelEntity::class,
                        FaceEntity::class,
                        WorkerHistoryEntity::class],
        version = 4,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
        abstract fun imageDao(): ImageDao
        abstract fun ocrTextDao(): OcrTextDao
        abstract fun barcodeDao(): BarcodeDao
        abstract fun imageLabelDao(): ImageLabelDao
        abstract fun faceDao(): FaceDao
        abstract fun workerHistoryDao(): WorkerHistoryDao

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
        }
}
