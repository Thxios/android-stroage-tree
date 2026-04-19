package com.thxios.storagetree.domain.usecase

import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.repository.StorageRepository
import javax.inject.Inject

class DeleteNodeUseCase @Inject constructor(private val repository: StorageRepository) {
    suspend operator fun invoke(node: FileNode): Result<Unit> = repository.deleteNode(node)
}
