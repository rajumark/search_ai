package com.photo.searchai.feature.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.photo.searchai.core.database.dao.BarcodeDao
import com.photo.searchai.core.database.entity.BarcodeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class BarcodeViewModel @Inject constructor(private val barcodeDao: BarcodeDao) : ViewModel() {

    val barcodes: Flow<PagingData<BarcodeEntity>> =
            Pager(
                            config = PagingConfig(pageSize = 20),
                            pagingSourceFactory = { barcodeDao.getAllBarcodesPaging() }
                    )
                    .flow
                    .cachedIn(viewModelScope)

    fun searchBarcodes(query: String): Flow<PagingData<BarcodeEntity>> {
        return Pager(
                        config = PagingConfig(pageSize = 20),
                        pagingSourceFactory = { barcodeDao.searchBarcodesPaging(query) }
                )
                .flow
                .cachedIn(viewModelScope)
    }
}
