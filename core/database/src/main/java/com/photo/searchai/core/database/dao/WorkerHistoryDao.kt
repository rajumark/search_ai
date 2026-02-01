package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.photo.searchai.core.database.entity.WorkerHistoryEntity
import com.photo.searchai.core.database.entity.WorkerStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerHistoryDao {

    @Insert suspend fun insert(history: WorkerHistoryEntity): Long

    @Update suspend fun update(history: WorkerHistoryEntity)

    @Query("SELECT * FROM worker_history ORDER BY startTime DESC")
    fun getAllHistory(): Flow<List<WorkerHistoryEntity>>

    @Query("SELECT * FROM worker_history ORDER BY startTime DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<WorkerHistoryEntity>>

    @Query("SELECT * FROM worker_history WHERE id = :id")
    suspend fun getById(id: Long): WorkerHistoryEntity?

    @Query("SELECT * FROM worker_history WHERE status = :status ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestByStatus(status: WorkerStatus): WorkerHistoryEntity?

    @Query("SELECT * FROM worker_history WHERE status = 'RUNNING' ORDER BY startTime DESC LIMIT 1")
    suspend fun getCurrentRunning(): WorkerHistoryEntity?

    @Query(
            "UPDATE worker_history SET status = :status, endTime = :endTime, durationMs = :durationMs, errorMessage = :errorMessage WHERE id = :id"
    )
    suspend fun updateStatus(
            id: Long,
            status: WorkerStatus,
            endTime: Long,
            durationMs: Long,
            errorMessage: String? = null
    )

    @Query(
            "UPDATE worker_history SET imagesProcessedOcr = :ocr, imagesProcessedBarcode = :barcode, imagesProcessedLabel = :label WHERE id = :id"
    )
    suspend fun updateProcessedCounts(id: Long, ocr: Int, barcode: Int, label: Int)

    @Query("DELETE FROM worker_history WHERE startTime < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("SELECT COUNT(*) FROM worker_history") suspend fun getCount(): Int

    @Query("SELECT * FROM worker_history ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatest(): WorkerHistoryEntity?

    @Query("SELECT * FROM worker_history WHERE isScheduledRun = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestScheduledRun(): WorkerHistoryEntity?
}
