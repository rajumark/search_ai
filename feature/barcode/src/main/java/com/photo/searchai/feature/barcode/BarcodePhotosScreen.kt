package com.photo.searchai.feature.barcode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.photo.searchai.core.database.entity.BarcodeEntity

/** Barcode Photos screen. Displays all detected barcodes from photos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodePhotosScreen(onNavigateBack: () -> Unit, viewModel: BarcodeViewModel = hiltViewModel()) {
    val barcodes = viewModel.barcodes.collectAsLazyPagingItems()

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Barcode Photos") },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Back"
                                )
                            }
                        }
                )
            }
    ) { innerPadding ->
        if (barcodes.itemCount == 0) {
            Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
            ) {
                Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                            text = "No barcodes found",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                            text =
                                    "We haven't found any barcodes in your photos yet. Make sure your photos are being indexed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = innerPadding) {
                items(count = barcodes.itemCount, key = barcodes.itemKey { it.id }) { index ->
                    val barcode = barcodes[index]
                    if (barcode != null) {
                        BarcodeItem(barcode = barcode)
                        HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeItem(barcode: BarcodeEntity) {
    ListItem(
            headlineContent = { Text(text = barcode.displayValue, fontWeight = FontWeight.Medium) },
            supportingContent = {
                Text(
                        text = "${barcode.formatName} • ID: ${barcode.mediaStoreId}",
                        style = MaterialTheme.typography.bodySmall
                )
            },
            leadingContent = {
                Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.clickable { /* TODO: Open photo */}
    )
}
