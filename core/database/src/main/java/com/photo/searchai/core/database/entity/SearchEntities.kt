package com.photo.searchai.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "search_suggestions")
data class SearchSuggestionEntity(@PrimaryKey val text: String, val frequency: Int)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
        @PrimaryKey val query: String,
        val timestamp: Long = System.currentTimeMillis()
)
