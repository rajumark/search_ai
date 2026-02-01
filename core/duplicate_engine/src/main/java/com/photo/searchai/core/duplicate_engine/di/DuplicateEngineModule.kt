package com.photo.searchai.core.duplicate_engine.di

import com.photo.searchai.core.duplicate_engine.DuplicateEngine
import com.photo.searchai.core.duplicate_engine.DuplicateEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DuplicateEngineModule {

    @Binds
    @Singleton
    abstract fun bindDuplicateEngine(
        impl: DuplicateEngineImpl
    ): DuplicateEngine
}
