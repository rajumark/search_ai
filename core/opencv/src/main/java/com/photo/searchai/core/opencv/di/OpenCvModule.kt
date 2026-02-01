package com.photo.searchai.core.opencv.di

import com.photo.searchai.core.opencv.BlurDetector
import com.photo.searchai.core.opencv.LaplacianBlurDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for OpenCV-based image analysis.
 * 
 * Binds the BlurDetector interface to the LaplacianBlurDetector implementation.
 * Feature modules should depend only on the BlurDetector interface.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OpenCvModule {

    @Binds
    abstract fun bindBlurDetector(
        impl: LaplacianBlurDetector
    ): BlurDetector
}
