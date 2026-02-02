package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.ProcessingSnapshotEntity
import com.photo.searchai.domain.model.FeatureType
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: ProcessingSnapshotEntity)

    @Query(
            "SELECT * FROM processing_snapshots WHERE featureType = :type ORDER BY createdAt DESC LIMIT 1"
    )
    fun getLatestSnapshot(type: FeatureType): Flow<ProcessingSnapshotEntity?>

    @Query(
            "SELECT * FROM processing_snapshots WHERE featureType = :type ORDER BY createdAt DESC LIMIT 1"
    )
    suspend fun getLatestSnapshotSync(type: FeatureType): ProcessingSnapshotEntity?

    @Query(
            "UPDATE processing_snapshots SET processedCount = :processed WHERE featureType = :type AND snapshotId = (SELECT snapshotId FROM processing_snapshots WHERE featureType = :type ORDER BY createdAt DESC LIMIT 1)"
    )
    suspend fun updateProgress(type: FeatureType, processed: Int)

    @Query("DELETE FROM processing_snapshots") suspend fun clearAll()
}
