package com.photo.searchai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity storing barcodes detected in images.
 * Each image can have multiple barcodes.
 */
@Entity(
    tableName = "barcodes",
    foreignKeys = [
        ForeignKey(
            entity = ImageEntity::class,
            parentColumns = ["mediaStoreId"],
            childColumns = ["mediaStoreId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mediaStoreId"])]
)
data class BarcodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long,
    val format: Int,         // BarcodeFormat enum value
    val formatName: String,  // Human readable format name (QR_CODE, UPC_A, etc.)
    val rawValue: String,    // Raw barcode value
    val displayValue: String, // Display value (may be different for some formats)
    val valueType: Int       // Barcode.TYPE_* value type (URL, EMAIL, PHONE, etc.)
)
