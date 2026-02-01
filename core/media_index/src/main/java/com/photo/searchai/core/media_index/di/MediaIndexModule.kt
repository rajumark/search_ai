package com.photo.searchai.core.media_index.di

import com.photo.searchai.core.media_index.MediaStoreIndexer
import com.photo.searchai.core.media_index.MediaStoreIndexerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaIndexModule {

    @Binds
    @Singleton
    abstract fun bindMediaStoreIndexer(
        impl: MediaStoreIndexerImpl
    ): MediaStoreIndexer
}
