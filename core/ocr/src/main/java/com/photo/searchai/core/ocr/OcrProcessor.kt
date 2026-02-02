package com.photo.searchai.core.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class OcrProcessor @Inject constructor(@ApplicationContext private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(uriString: String): String {
        return try {
            val uri = Uri.parse(uriString)
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            result.text
        } catch (e: Exception) {
            ""
        }
    }
}
