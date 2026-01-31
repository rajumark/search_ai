package com.photo.searchai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.photo.searchai.data.local.dao.BarcodeDao
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.ImageLabelDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.entity.BarcodeEntity
import com.photo.searchai.data.local.entity.ImageEntity
import com.photo.searchai.data.local.entity.ImageLabelEntity
import com.photo.searchai.data.local.entity.OcrTextEntity

/**
 * Room database for the photo search system.
 * Stores images, OCR text, barcodes, and image labels.
 */
@Database(
    entities = [
        ImageEntity::class, 
        OcrTextEntity::class,
        BarcodeEntity::class,
        ImageLabelEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun ocrTextDao(): OcrTextDao
    abstract fun barcodeDao(): BarcodeDao
    abstract fun imageLabelDao(): ImageLabelDao
    
    companion object {
        const val DATABASE_NAME = "photo_search_db"
        
        /**
         * Migration from version 1 to 2.
         * Adds barcodeParsed, labelParsed columns to images table.
         * Creates barcodes and image_labels tables.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to images table
                db.execSQL("ALTER TABLE images ADD COLUMN barcodeParsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE images ADD COLUMN labelParsed INTEGER NOT NULL DEFAULT 0")
                
                // Create barcodes table
                db.execSQL("""
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
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_barcodes_mediaStoreId ON barcodes(mediaStoreId)")
                
                // Create image_labels table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS image_labels (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaStoreId INTEGER NOT NULL,
                        label TEXT NOT NULL,
                        confidence REAL NOT NULL,
                        `index` INTEGER NOT NULL,
                        FOREIGN KEY(mediaStoreId) REFERENCES images(mediaStoreId) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_image_labels_mediaStoreId ON image_labels(mediaStoreId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_image_labels_label ON image_labels(label)")
            }
        }
    }
}

