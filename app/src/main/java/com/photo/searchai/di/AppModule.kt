package com.photo.searchai.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.photo.searchai.core.data.repository.SnapshotRepositoryImpl
import com.photo.searchai.core.database.AppDatabase
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.dao.SnapshotDao
import com.photo.searchai.domain.repository.SnapshotRepository
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "search_ai.db").build()
    }

    @Provides fun provideImageDao(db: AppDatabase): ImageDao = db.imageDao()

    @Provides fun provideSnapshotDao(db: AppDatabase): SnapshotDao = db.snapshotDao()

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideSnapshotRepository(
            snapshotDao: SnapshotDao,
            imageDao: ImageDao
    ): SnapshotRepository {
        return SnapshotRepositoryImpl(snapshotDao, imageDao)
    }
}
