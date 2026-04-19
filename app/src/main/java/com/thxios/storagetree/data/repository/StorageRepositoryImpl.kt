package com.thxios.storagetree.data.repository

import com.thxios.storagetree.data.scanner.FileScanner
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.NoSuchFileException
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

    override suspend fun deleteNode(node: FileNode): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(node.path)
            if (!file.exists()) Result.failure(NoSuchFileException(node.path))
            else if (file.deleteRecursively()) Result.success(Unit)
            else Result.failure(IOException("Failed to delete ${node.path}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
