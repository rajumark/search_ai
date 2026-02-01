package com.photo.searchai.core.datastore.di

import android.content.Context
import com.photo.searchai.core.datastore.OcrProgressDataStore
import com.photo.searchai.core.datastore.ScheduledWorkDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatastoreModule {

    @Provides
    @Singleton
    fun provideOcrProgressDataStore(@ApplicationContext context: Context): OcrProgressDataStore {
        return OcrProgressDataStore(context)
    }

    @Provides
    @Singleton
    fun provideScheduledWorkDataStore(@ApplicationContext context: Context): ScheduledWorkDataStore {
        return ScheduledWorkDataStore(context)
    }
}
