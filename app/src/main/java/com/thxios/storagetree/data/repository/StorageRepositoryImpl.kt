package com.thxios.storagetree.data.repository

import com.thxios.storagetree.data.scanner.FileScanner
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    private val scanner: FileScanner
) : StorageRepository {
    override fun scan(path: String): Flow<ScanState> {
        val file = File(path)
        if (!file.exists()) {
            return flow {
                emit(ScanState.Error("Path does not exist: $path"))
            }
        }
        return scanner.scan(path).catch { e ->
            emit(ScanState.Error(e.message ?: "Unknown error"))
        }
    }
}
