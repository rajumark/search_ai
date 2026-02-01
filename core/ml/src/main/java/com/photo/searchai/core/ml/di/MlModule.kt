package com.photo.searchai.core.ml.di

import com.photo.searchai.core.ml.BarcodeProcessor
import com.photo.searchai.core.ml.FaceDetectionProcessor
import com.photo.searchai.core.ml.ImageLabelProcessor
import com.photo.searchai.core.ml.OcrProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlModule {

    @Provides
    @Singleton
    fun provideOcrProcessor(): OcrProcessor {
        return OcrProcessor()
    }

    @Provides
    @Singleton
    fun provideBarcodeProcessor(): BarcodeProcessor {
        return BarcodeProcessor()
    }

    @Provides
    @Singleton
    fun provideFaceDetectionProcessor(): FaceDetectionProcessor {
        return FaceDetectionProcessor()
    }

    @Provides
    @Singleton
    fun provideImageLabelProcessor(): ImageLabelProcessor {
        return ImageLabelProcessor()
    }
}
