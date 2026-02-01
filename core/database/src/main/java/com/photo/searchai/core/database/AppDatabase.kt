package com.photo.searchai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photo.searchai.core.database.dao.BarcodeDao
import com.photo.searchai.core.database.dao.CleanupDao
import com.photo.searchai.core.database.dao.DuplicateDao
import com.photo.searchai.core.database.dao.ExifDao
import com.photo.searchai.core.database.dao.FaceDao
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.dao.ImageLabelDao
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
        }
}
