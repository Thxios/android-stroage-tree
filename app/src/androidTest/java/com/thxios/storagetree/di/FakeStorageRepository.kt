package com.thxios.storagetree.di

import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class FakeStorageRepository @Inject constructor() : StorageRepository {
    override fun scan(path: String): Flow<ScanState> =
        flowOf(ScanState.Done(FileNode(name = "root", path = path, sizeBytes = 0L, isDirectory = true)))

    override suspend fun deleteNode(node: FileNode): Result<Unit> = Result.success(Unit)
}
