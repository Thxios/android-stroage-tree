package com.thxios.storagetree.domain.repository

import com.thxios.storagetree.domain.model.ScanState
import kotlinx.coroutines.flow.Flow

interface StorageRepository {
    fun scan(path: String): Flow<ScanState>
}
