package com.photo.searchai.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.data.local.entity.ImageEntity
import com.photo.searchai.data.local.entity.OcrTextEntity

/**
 * Room database for the OCR indexing system.
 */
@Database(
    entities = [ImageEntity::class, OcrTextEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun ocrTextDao(): OcrTextDao
    
    companion object {
        const val DATABASE_NAME = "photo_search_db"
    }
}
