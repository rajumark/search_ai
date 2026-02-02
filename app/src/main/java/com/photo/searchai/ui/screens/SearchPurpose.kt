package com.photo.searchai.ui.screens

sealed class SearchPurpose {
    data object GeneralSearch : SearchPurpose()
    data object Favorites : SearchPurpose()
    data class AlbumSearch(val bucketId: Long, val bucketName: String) : SearchPurpose()
    data class GroupingSearch(val query: String) : SearchPurpose()
    
    fun getInitialQuery(): String {
        return when (this) {
            is Favorites -> "is favorite"
            is GroupingSearch -> query
            else -> ""
        }
    }
    
    fun getPlaceholderText(): String {
        return when (this) {
            is Favorites -> "Favorite images"
            is AlbumSearch -> "Search ${bucketName}"
            else -> "Search photos by text"
        }
    }
    
    fun shouldShowBucketName(): Boolean {
        return when (this) {
            is AlbumSearch -> true
            else -> false
        }
    }
    
    fun getBucketNameForDisplay(): String {
        return when (this) {
            is AlbumSearch -> bucketName
            else -> ""
        }
    }
    
    fun getBucketIdForSearch(): Long {
        return when (this) {
            is AlbumSearch -> bucketId
            else -> -1L
        }
    }
}
