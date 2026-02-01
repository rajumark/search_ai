package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.VaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultEntry(entry: VaultEntity)

    @Query("SELECT * FROM vault_entries")
    suspend fun getAllVaultEntries(): List<VaultEntity>

    @Query("SELECT * FROM vault_entries")
    fun getAllVaultEntriesFlow(): Flow<List<VaultEntity>>

    @Query("SELECT * FROM vault_entries WHERE mediaStoreId = :mediaStoreId")
    suspend fun getVaultEntry(mediaStoreId: Long): VaultEntity?

    @Query("DELETE FROM vault_entries WHERE mediaStoreId = :mediaStoreId")
    suspend fun removeVaultEntry(mediaStoreId: Long)
}
