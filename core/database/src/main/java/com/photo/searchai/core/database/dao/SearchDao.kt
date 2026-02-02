package com.photo.searchai.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photo.searchai.core.database.entity.RecentSearchEntity
import com.photo.searchai.core.database.entity.SearchSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchDao {
    @Query(
            "SELECT * FROM search_suggestions WHERE text LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit"
    )
    suspend fun getSuggestionsByPrefix(prefix: String, limit: Int): List<SearchSuggestionEntity>

    @Query("SELECT * FROM search_suggestions ORDER BY frequency DESC LIMIT :limit")
    suspend fun getGlobalTopSuggestions(limit: Int): List<SearchSuggestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestions(suggestions: List<SearchSuggestionEntity>)

    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentSearches(limit: Int): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(recentSearch: RecentSearchEntity)

    @Query(
            "DELETE FROM recent_searches WHERE query NOT IN (SELECT query FROM recent_searches ORDER BY timestamp DESC LIMIT :limit)"
    )
    suspend fun trimRecentSearches(limit: Int)

    @Query("SELECT * FROM ocr_results WHERE ocrText LIKE '%' || :query || '%' LIMIT 100")
    suspend fun getRawOcrResultsForCoOccurrence(
            query: String
    ): List<com.photo.searchai.core.database.entity.OcrEntity>
}
