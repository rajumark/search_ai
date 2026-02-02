package com.photo.searchai.core.ocr

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

data class LabelResult(val text: String, val confidence: Float, val index: Int)

@Singleton
class LabelingProcessor @Inject constructor(@ApplicationContext private val context: Context) {
    private val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(uriString: String): List<LabelResult> {
        return try {
            val uri = Uri.parse(uriString)
            val image = InputImage.fromFilePath(context, uri)
            val labels = labeler.process(image).await()
            labels.map { LabelResult(it.text, it.confidence, it.index) }
        } catch (e: Exception) {
            Log.e(TAG, "Image labeling failed for uri=$uriString", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "LabelingProcessor"
    }
}
