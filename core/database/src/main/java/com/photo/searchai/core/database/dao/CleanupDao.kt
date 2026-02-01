package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.CleanupCandidateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CleanupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidate(candidate: CleanupCandidateEntity)

    @Query("SELECT * FROM cleanup_candidates")
    suspend fun getAllCandidates(): List<CleanupCandidateEntity>

    @Query("SELECT * FROM cleanup_candidates")
    fun getAllCandidatesFlow(): Flow<List<CleanupCandidateEntity>>

    @Query("DELETE FROM cleanup_candidates WHERE mediaStoreId = :mediaStoreId")
    suspend fun deleteCandidate(mediaStoreId: Long)

    @Query("DELETE FROM cleanup_candidates")
    suspend fun clearAllCandidates()
}
