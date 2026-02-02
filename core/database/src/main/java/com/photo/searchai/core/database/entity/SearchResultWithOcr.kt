package com.photo.searchai.core.database.entity

import androidx.room.Embedded

data class SearchResultWithOcr(@Embedded val image: ImageEntity, val ocrText: String?)
