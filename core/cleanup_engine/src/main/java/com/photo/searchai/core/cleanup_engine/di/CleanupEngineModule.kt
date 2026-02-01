package com.photo.searchai.core.cleanup_engine.di

import com.photo.searchai.core.cleanup_engine.CleanupEngine
import com.photo.searchai.core.cleanup_engine.CleanupEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CleanupEngineModule {

    @Binds
    @Singleton
    abstract fun bindCleanupEngine(
        impl: CleanupEngineImpl
    ): CleanupEngine
}
