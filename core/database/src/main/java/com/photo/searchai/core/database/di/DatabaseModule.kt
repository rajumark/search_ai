package com.photo.searchai.core.database.di

import android.content.Context
import androidx.room.Room
import com.photo.searchai.core.database.AppDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
                .addMigrations(
                        AppDatabase.MIGRATION_1_2,
                        AppDatabase.MIGRATION_2_3,
                        AppDatabase.MIGRATION_3_4,
                        AppDatabase.MIGRATION_4_5,
                        AppDatabase.MIGRATION_5_6
                )
                .build()
    }

    @Provides
    @Singleton
    fun provideImageDao(database: AppDatabase): ImageDao {
        return database.imageDao()
    }

    @Provides
    @Singleton
    fun provideOcrTextDao(database: AppDatabase): OcrTextDao {
        return database.ocrTextDao()
    }

    @Provides
    @Singleton
    fun provideBarcodeDao(database: AppDatabase): BarcodeDao {
        return database.barcodeDao()
    }

    @Provides
    @Singleton
    fun provideImageLabelDao(database: AppDatabase): ImageLabelDao {
        return database.imageLabelDao()
    }

    @Provides
    @Singleton
    fun provideFaceDao(database: AppDatabase): FaceDao {
        return database.faceDao()
    }

    @Provides
    @Singleton
    fun provideImageQualityDao(database: AppDatabase): ImageQualityDao {
        return database.imageQualityDao()
    }

    @Provides
    @Singleton
    fun provideWorkerHistoryDao(database: AppDatabase): WorkerHistoryDao {
        return database.workerHistoryDao()
    }

    @Provides
    @Singleton
    fun provideExifDao(database: AppDatabase): ExifDao {
        return database.exifDao()
    }

    @Provides
    @Singleton
    fun provideDuplicateDao(database: AppDatabase): DuplicateDao {
        return database.duplicateDao()
    }

    @Provides
    @Singleton
    fun provideSmartAlbumDao(database: AppDatabase): SmartAlbumDao {
        return database.smartAlbumDao()
    }

    @Provides
    @Singleton
    fun provideCleanupDao(database: AppDatabase): CleanupDao {
        return database.cleanupDao()
    }

    @Provides
    @Singleton
    fun provideVaultDao(database: AppDatabase): VaultDao {
        return database.vaultDao()
    }
}
