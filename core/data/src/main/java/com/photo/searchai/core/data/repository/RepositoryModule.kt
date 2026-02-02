package com.photo.searchai.core.data.repository

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Usually we bind interfaces to implementations, but here we provide the class directly via
    // @Inject
    // If we had an interface IMediaRepository, we would bind it here.
    // Since we are using the class directly with @Inject and @Singleton, Hilt will find it.
}
