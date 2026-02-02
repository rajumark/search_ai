package com.photo.searchai.core.database.di

import android.content.Context
import androidx.room.Room
import com.photo.searchai.core.database.AppDatabase
import com.photo.searchai.core.database.dao.ImageDao
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
        return Room.databaseBuilder(context, AppDatabase::class.java, "photo_search_ai_db")
                .fallbackToDestructiveMigration()
                .build()
    }

    @Provides
    fun provideImageDao(database: AppDatabase): ImageDao {
        return database.imageDao()
    }

    @Provides
    fun provideSearchDao(database: AppDatabase): com.photo.searchai.core.database.dao.SearchDao {
        return database.searchDao()
    }
}
