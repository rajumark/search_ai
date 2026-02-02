package com.photo.searchai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.photo.searchai.core.database.dao.ImageDao
import com.photo.searchai.core.database.entity.ImageEntity

@Database(
        entities =
                [
                        ImageEntity::class,
                        com.photo.searchai.core.database.entity.OcrEntity::class,
                        com.photo.searchai.core.database.entity.SearchSuggestionEntity::class,
                        com.photo.searchai.core.database.entity.RecentSearchEntity::class],
        version = 4,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun searchDao(): com.photo.searchai.core.database.dao.SearchDao
}
