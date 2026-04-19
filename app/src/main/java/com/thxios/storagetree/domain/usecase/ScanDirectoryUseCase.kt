package com.thxios.storagetree.domain.usecase

import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanDirectoryUseCase @Inject constructor(
    private val repository: StorageRepository
) {
    operator fun invoke(path: String): Flow<ScanState> = repository.scan(path)
}
