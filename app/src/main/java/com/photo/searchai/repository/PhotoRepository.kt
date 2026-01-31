package com.photo.searchai.repository

import com.photo.searchai.datasource.PhotoDataSource
import javax.inject.Inject

/**
 * Repository interface for photo operations.
 */
interface PhotoRepository {
    /**
     * Gets the total count of photos on the device.
     */
    suspend fun getTotalPhotoCount(): Int
}

/**
 * Implementation of [PhotoRepository] that uses [PhotoDataSource].
 */
class PhotoRepositoryImpl @Inject constructor(
    private val photoDataSource: PhotoDataSource
) : PhotoRepository {
    
    override suspend fun getTotalPhotoCount(): Int {
        return photoDataSource.getTotalPhotoCount()
    }
}
