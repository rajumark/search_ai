package com.photo.searchai.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.progressDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "image_processing_progress")

enum class ProcessingStage {
    IDLE,
    OCR,
    BARCODE,
    LABELING,
    FACE_DETECTION,
    QUALITY_ANALYSIS,
    COMPLETE
}

data class BenchmarkData(
        val startTime: Long = 0L,
        val timeTo10Percent: Long = 0L,
        val timeTo30Percent: Long = 0L,
        val timeTo50Percent: Long = 0L,
        val timeTo70Percent: Long = 0L,
        val timeTo100Percent: Long = 0L,
        val totalImagesProcessed: Int = 0,
        val isComplete: Boolean = false
) {
    val averageTimePerImageMs: Long
        get() =
                if (totalImagesProcessed > 0 && timeTo100Percent > 0) {
                    timeTo100Percent / totalImagesProcessed
                } else 0L

    val formattedAverageTime: String
        get() {
            val avgMs = averageTimePerImageMs
            return when {
                avgMs < 1000 -> "${avgMs}ms"
                avgMs < 60000 -> String.format("%.1fs", avgMs / 1000.0)
                else -> String.format("%.1fmin", avgMs / 60000.0)
            }
        }

    fun formatMilestoneTime(milestoneMs: Long): String {
        if (milestoneMs <= 0) return "—"
        return when {
            milestoneMs < 1000 -> "${milestoneMs}ms"
            milestoneMs < 60000 -> String.format("%.1fs", milestoneMs / 1000.0)
            milestoneMs < 3600000 -> String.format("%.1fmin", milestoneMs / 60000.0)
            else -> String.format("%.1fh", milestoneMs / 3600000.0)
        }
    }

    val milestones: List<Pair<String, String>>
        get() =
                listOf(
                        "10%" to formatMilestoneTime(timeTo10Percent),
                        "30%" to formatMilestoneTime(timeTo30Percent),
                        "50%" to formatMilestoneTime(timeTo50Percent),
                        "70%" to formatMilestoneTime(timeTo70Percent),
                        "100%" to formatMilestoneTime(timeTo100Percent)
                )
}

data class ProcessingProgress(
        val totalImages: Int = 0,
        val ocrParsed: Int = 0,
        val ocrPending: Int = 0,
        val barcodeParsed: Int = 0,
        val barcodePending: Int = 0,
        val labelParsed: Int = 0,
        val labelPending: Int = 0,
        val faceParsed: Int = 0,
        val facePending: Int = 0,
        val qualityParsed: Int = 0,
        val qualityPending: Int = 0,
        val currentStage: ProcessingStage = ProcessingStage.IDLE,
        val lastUpdated: Long = 0,
        val benchmarkData: BenchmarkData = BenchmarkData()
) {
    val overallProgress: Float
        get() {
            val totalWork = totalImages * 5 // 5 stages now
            if (totalWork <= 0) return 0f
            val completedWork = ocrParsed + barcodeParsed + labelParsed + faceParsed + qualityParsed
            return completedWork.toFloat() / totalWork
        }

    val ocrProgress: Float
        get() = if (totalImages > 0) ocrParsed.toFloat() / totalImages else 0f

    val barcodeProgress: Float
        get() = if (totalImages > 0) barcodeParsed.toFloat() / totalImages else 0f

    val labelProgress: Float
        get() = if (totalImages > 0) labelParsed.toFloat() / totalImages else 0f

    val faceProgress: Float
        get() = if (totalImages > 0) faceParsed.toFloat() / totalImages else 0f

    val qualityProgress: Float
        get() = if (totalImages > 0) qualityParsed.toFloat() / totalImages else 0f

    val isComplete: Boolean
        get() =
                totalImages > 0 &&
                        ocrPending == 0 &&
                        barcodePending == 0 &&
                        labelPending == 0 &&
                        facePending == 0 &&
                        qualityPending == 0

    val isProcessing: Boolean
        get() = currentStage != ProcessingStage.IDLE && currentStage != ProcessingStage.COMPLETE
}

typealias OcrProgress = ProcessingProgress

class OcrProgressDataStore @Inject constructor(private val context: Context) {
    private object Keys {
        val TOTAL_IMAGES = intPreferencesKey("total_images")
        val OCR_PARSED = intPreferencesKey("ocr_parsed")
        val OCR_PENDING = intPreferencesKey("ocr_pending")
        val BARCODE_PARSED = intPreferencesKey("barcode_parsed")
        val BARCODE_PENDING = intPreferencesKey("barcode_pending")
        val LABEL_PARSED = intPreferencesKey("label_parsed")
        val LABEL_PENDING = intPreferencesKey("label_pending")
        val FACE_PARSED = intPreferencesKey("face_parsed")
        val FACE_PENDING = intPreferencesKey("face_pending")
        val QUALITY_PARSED = intPreferencesKey("quality_parsed")
        val QUALITY_PENDING = intPreferencesKey("quality_pending")
        val CURRENT_STAGE = stringPreferencesKey("current_stage")
        val LAST_UPDATED = longPreferencesKey("last_updated")
        val PARSED_IMAGES = intPreferencesKey("parsed_images")
        val PENDING_IMAGES = intPreferencesKey("pending_images")

        val BENCHMARK_START_TIME = longPreferencesKey("benchmark_start_time")
        val BENCHMARK_TIME_10 = longPreferencesKey("benchmark_time_10")
        val BENCHMARK_TIME_30 = longPreferencesKey("benchmark_time_30")
        val BENCHMARK_TIME_50 = longPreferencesKey("benchmark_time_50")
        val BENCHMARK_TIME_70 = longPreferencesKey("benchmark_time_70")
        val BENCHMARK_TIME_100 = longPreferencesKey("benchmark_time_100")
        val BENCHMARK_TOTAL_PROCESSED = intPreferencesKey("benchmark_total_processed")
        val BENCHMARK_COMPLETE = intPreferencesKey("benchmark_complete")
    }

    val progressFlow: Flow<ProcessingProgress> =
            context.progressDataStore.data.map { preferences ->
                ProcessingProgress(
                        totalImages = preferences[Keys.TOTAL_IMAGES] ?: 0,
                        ocrParsed = preferences[Keys.OCR_PARSED]
                                        ?: preferences[Keys.PARSED_IMAGES] ?: 0,
                        ocrPending = preferences[Keys.OCR_PENDING]
                                        ?: preferences[Keys.PENDING_IMAGES] ?: 0,
                        barcodeParsed = preferences[Keys.BARCODE_PARSED] ?: 0,
                        barcodePending = preferences[Keys.BARCODE_PENDING] ?: 0,
                        labelParsed = preferences[Keys.LABEL_PARSED] ?: 0,
                        labelPending = preferences[Keys.LABEL_PENDING] ?: 0,
                        faceParsed = preferences[Keys.FACE_PARSED] ?: 0,
                        facePending = preferences[Keys.FACE_PENDING] ?: 0,
                        qualityParsed = preferences[Keys.QUALITY_PARSED] ?: 0,
                        qualityPending = preferences[Keys.QUALITY_PENDING] ?: 0,
                        currentStage =
                                try {
                                    ProcessingStage.valueOf(
                                            preferences[Keys.CURRENT_STAGE] ?: "IDLE"
                                    )
                                } catch (e: Exception) {
                                    ProcessingStage.IDLE
                                },
                        lastUpdated = preferences[Keys.LAST_UPDATED] ?: 0,
                        benchmarkData =
                                BenchmarkData(
                                        startTime = preferences[Keys.BENCHMARK_START_TIME] ?: 0L,
                                        timeTo10Percent = preferences[Keys.BENCHMARK_TIME_10] ?: 0L,
                                        timeTo30Percent = preferences[Keys.BENCHMARK_TIME_30] ?: 0L,
                                        timeTo50Percent = preferences[Keys.BENCHMARK_TIME_50] ?: 0L,
                                        timeTo70Percent = preferences[Keys.BENCHMARK_TIME_70] ?: 0L,
                                        timeTo100Percent = preferences[Keys.BENCHMARK_TIME_100]
                                                        ?: 0L,
                                        totalImagesProcessed =
                                                preferences[Keys.BENCHMARK_TOTAL_PROCESSED] ?: 0,
                                        isComplete = (preferences[Keys.BENCHMARK_COMPLETE]
                                                        ?: 0) == 1
                                )
                )
            }

    val benchmarkFlow: Flow<BenchmarkData> =
            context.progressDataStore.data.map { preferences ->
                BenchmarkData(
                        startTime = preferences[Keys.BENCHMARK_START_TIME] ?: 0L,
                        timeTo10Percent = preferences[Keys.BENCHMARK_TIME_10] ?: 0L,
                        timeTo30Percent = preferences[Keys.BENCHMARK_TIME_30] ?: 0L,
                        timeTo50Percent = preferences[Keys.BENCHMARK_TIME_50] ?: 0L,
                        timeTo70Percent = preferences[Keys.BENCHMARK_TIME_70] ?: 0L,
                        timeTo100Percent = preferences[Keys.BENCHMARK_TIME_100] ?: 0L,
                        totalImagesProcessed = preferences[Keys.BENCHMARK_TOTAL_PROCESSED] ?: 0,
                        isComplete = (preferences[Keys.BENCHMARK_COMPLETE] ?: 0) == 1
                )
            }

    suspend fun updateProgress(total: Int, parsed: Int, pending: Int) {
        updateOcrProgress(total, parsed, pending)
    }

    suspend fun updateOcrProgress(total: Int, parsed: Int, pending: Int) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.OCR_PARSED] = parsed
            preferences[Keys.OCR_PENDING] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun updateBarcodeProgress(total: Int, parsed: Int, pending: Int) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.BARCODE_PARSED] = parsed
            preferences[Keys.BARCODE_PENDING] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun updateLabelProgress(total: Int, parsed: Int, pending: Int) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.LABEL_PARSED] = parsed
            preferences[Keys.LABEL_PENDING] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun updateFaceProgress(total: Int, parsed: Int, pending: Int) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.FACE_PARSED] = parsed
            preferences[Keys.FACE_PENDING] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun updateQualityProgress(total: Int, parsed: Int, pending: Int) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.QUALITY_PARSED] = parsed
            preferences[Keys.QUALITY_PENDING] = pending
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun updateCurrentStage(stage: ProcessingStage) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.CURRENT_STAGE] = stage.name
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun updateAllProgress(
            total: Int,
            ocrParsed: Int,
            ocrPending: Int,
            barcodeParsed: Int,
            barcodePending: Int,
            labelParsed: Int,
            labelPending: Int,
            faceParsed: Int,
            facePending: Int,
            qualityParsed: Int,
            qualityPending: Int,
            currentStage: ProcessingStage
    ) {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.TOTAL_IMAGES] = total
            preferences[Keys.OCR_PARSED] = ocrParsed
            preferences[Keys.OCR_PENDING] = ocrPending
            preferences[Keys.BARCODE_PARSED] = barcodeParsed
            preferences[Keys.BARCODE_PENDING] = barcodePending
            preferences[Keys.LABEL_PARSED] = labelParsed
            preferences[Keys.LABEL_PENDING] = labelPending
            preferences[Keys.FACE_PARSED] = faceParsed
            preferences[Keys.FACE_PENDING] = facePending
            preferences[Keys.QUALITY_PARSED] = qualityParsed
            preferences[Keys.QUALITY_PENDING] = qualityPending
            preferences[Keys.CURRENT_STAGE] = currentStage.name
            preferences[Keys.LAST_UPDATED] = System.currentTimeMillis()
        }
    }

    suspend fun startBenchmark() {
        context.progressDataStore.edit { preferences ->
            preferences[Keys.BENCHMARK_START_TIME] = System.currentTimeMillis()
            preferences[Keys.BENCHMARK_TIME_10] = 0L
            preferences[Keys.BENCHMARK_TIME_30] = 0L
            preferences[Keys.BENCHMARK_TIME_50] = 0L
            preferences[Keys.BENCHMARK_TIME_70] = 0L
            preferences[Keys.BENCHMARK_TIME_100] = 0L
            preferences[Keys.BENCHMARK_TOTAL_PROCESSED] = 0
            preferences[Keys.BENCHMARK_COMPLETE] = 0
        }
    }

    suspend fun recordMilestone(percentComplete: Int, totalProcessed: Int) {
        context.progressDataStore.edit { preferences ->
            val startTime = preferences[Keys.BENCHMARK_START_TIME] ?: return@edit
            val elapsed = System.currentTimeMillis() - startTime

            when (percentComplete) {
                10 ->
                        if ((preferences[Keys.BENCHMARK_TIME_10] ?: 0L) == 0L) {
                            preferences[Keys.BENCHMARK_TIME_10] = elapsed
                        }
                30 ->
                        if ((preferences[Keys.BENCHMARK_TIME_30] ?: 0L) == 0L) {
                            preferences[Keys.BENCHMARK_TIME_30] = elapsed
                        }
                50 ->
                        if ((preferences[Keys.BENCHMARK_TIME_50] ?: 0L) == 0L) {
                            preferences[Keys.BENCHMARK_TIME_50] = elapsed
                        }
                70 ->
                        if ((preferences[Keys.BENCHMARK_TIME_70] ?: 0L) == 0L) {
                            preferences[Keys.BENCHMARK_TIME_70] = elapsed
                        }
                100 -> {
                    preferences[Keys.BENCHMARK_TIME_100] = elapsed
                    preferences[Keys.BENCHMARK_COMPLETE] = 1
                }
            }
            preferences[Keys.BENCHMARK_TOTAL_PROCESSED] = totalProcessed
        }
    }

    suspend fun completeBenchmark(totalProcessed: Int) {
        context.progressDataStore.edit { preferences ->
            val startTime = preferences[Keys.BENCHMARK_START_TIME] ?: return@edit
            val elapsed = System.currentTimeMillis() - startTime
            preferences[Keys.BENCHMARK_TIME_100] = elapsed
            preferences[Keys.BENCHMARK_TOTAL_PROCESSED] = totalProcessed
            preferences[Keys.BENCHMARK_COMPLETE] = 1
        }
    }

    suspend fun clearProgress() {
        context.progressDataStore.edit { preferences -> preferences.clear() }
    }

    suspend fun clearBenchmark() {
        context.progressDataStore.edit { preferences ->
            preferences.remove(Keys.BENCHMARK_START_TIME)
            preferences.remove(Keys.BENCHMARK_TIME_10)
            preferences.remove(Keys.BENCHMARK_TIME_30)
            preferences.remove(Keys.BENCHMARK_TIME_50)
            preferences.remove(Keys.BENCHMARK_TIME_70)
            preferences.remove(Keys.BENCHMARK_TIME_100)
            preferences.remove(Keys.BENCHMARK_TOTAL_PROCESSED)
            preferences.remove(Keys.BENCHMARK_COMPLETE)
        }
    }
}
