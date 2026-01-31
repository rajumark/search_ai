package com.photo.searchai.di

import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.photo.searchai.data.datastore.OcrProgressDataStore
import com.photo.searchai.data.local.AppDatabase
import com.photo.searchai.data.local.dao.ImageDao
import com.photo.searchai.data.local.dao.OcrTextDao
import com.photo.searchai.datasource.PhotoDataSource
import com.photo.searchai.ocr.OcrProcessor
import com.photo.searchai.repository.OcrRepository
import com.photo.searchai.repository.OcrRepositoryImpl
import com.photo.searchai.repository.PhotoRepository
import com.photo.searchai.repository.PhotoRepositoryImpl
import com.photo.searchai.worker.WorkManagerHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun providePhotoDataSource(contentResolver: ContentResolver): PhotoDataSource {
        return PhotoDataSource(contentResolver)
    }

    @Provides
    @Singleton
    fun providePhotoRepository(photoDataSource: PhotoDataSource): PhotoRepository {
        return PhotoRepositoryImpl(photoDataSource)
    }
    
    // Room Database
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
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
    
    // DataStore
    @Provides
    @Singleton
    fun provideOcrProgressDataStore(@ApplicationContext context: Context): OcrProgressDataStore {
        return OcrProgressDataStore(context)
    }
    
    // WorkManager
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideWorkManagerHelper(@ApplicationContext context: Context): WorkManagerHelper {
        return WorkManagerHelper(context)
    }
    
    // OCR
    @Provides
    @Singleton
    fun provideOcrProcessor(): OcrProcessor {
        return OcrProcessor()
    }
    
    // OCR Repository
    @Provides
    @Singleton
    fun provideOcrRepository(
        imageDao: ImageDao,
        ocrTextDao: OcrTextDao,
        photoDataSource: PhotoDataSource,
        workManagerHelper: WorkManagerHelper,
        progressDataStore: OcrProgressDataStore
    ): OcrRepository {
        return OcrRepositoryImpl(
            imageDao,
            ocrTextDao,
            photoDataSource,
            workManagerHelper,
            progressDataStore
        )
    }
}

