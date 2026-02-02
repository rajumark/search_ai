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
                        com.photo.searchai.core.database.entity.RecentSearchEntity::class,
                        com.photo.searchai.core.database.entity.GroupEntity::class,
                        com.photo.searchai.core.database.entity.KeywordEntity::class,
                        com.photo.searchai.core.database.entity.GroupImageEntity::class,
                        com.photo.searchai.core.database.entity.ImageLabelEntity::class],
        version = 6,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun imageDao(): ImageDao
    abstract fun searchDao(): com.photo.searchai.core.database.dao.SearchDao
    abstract fun groupDao(): com.photo.searchai.core.database.dao.GroupDao
    abstract fun imageLabelDao(): com.photo.searchai.core.database.dao.ImageLabelDao
}
