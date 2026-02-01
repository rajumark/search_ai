package com.photo.searchai.core.rules_engine.di

import com.photo.searchai.core.rules_engine.RulesEngine
import com.photo.searchai.core.rules_engine.RulesEngineImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RulesEngineModule {

    @Binds
    @Singleton
    abstract fun bindRulesEngine(
        impl: RulesEngineImpl
    ): RulesEngine
}
