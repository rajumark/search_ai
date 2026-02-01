package com.photo.searchai.core.metadata_index.di

import com.photo.searchai.core.metadata_index.MetadataIndexer
import com.photo.searchai.core.metadata_index.MetadataIndexerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MetadataIndexModule {

    @Binds
    @Singleton
    abstract fun bindMetadataIndexer(
        impl: MetadataIndexerImpl
    ): MetadataIndexer
}
