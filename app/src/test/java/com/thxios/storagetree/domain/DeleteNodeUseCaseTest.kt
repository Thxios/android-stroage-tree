package com.thxios.storagetree.domain

import com.thxios.storagetree.data.repository.StorageRepositoryImpl
import com.thxios.storagetree.data.scanner.FileScanner
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.usecase.DeleteNodeUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DeleteNodeUseCaseTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val repository = StorageRepositoryImpl(FileScanner())
    private val useCase = DeleteNodeUseCase(repository)

    private fun fileNode(path: String, isDirectory: Boolean = false) = FileNode(
        name = path.substringAfterLast('/'),
        path = path,
        sizeBytes = 0L,
        isDirectory = isDirectory
    )

    @Test
    fun `delete existing file returns success and file no longer exists`() = runTest {
        val file = tempFolder.newFile("delete_me.txt")
        file.writeText("hello")
        assertTrue(file.exists())

        val result = useCase(fileNode(file.absolutePath))

        assertTrue(result.isSuccess)
        assertFalse(file.exists())
    }

    @Test
    fun `delete directory with children recursively returns success and dir gone`() = runTest {
        val dir = tempFolder.newFolder("delete_dir")
        dir.resolve("child.txt").writeText("child")
        dir.resolve("sub").mkdir()
        dir.resolve("sub/nested.txt").writeText("nested")
        assertTrue(dir.exists())

        val result = useCase(fileNode(dir.absolutePath, isDirectory = true))

        assertTrue(result.isSuccess)
        assertFalse(dir.exists())
    }

    @Test
    fun `delete non-existent path returns failure`() = runTest {
        val fakePath = tempFolder.root.absolutePath + "/does_not_exist.txt"

        val result = useCase(fileNode(fakePath))

        assertTrue(result.isFailure)
    }
}

class StorageRepositoryImplDeleteTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val repository = StorageRepositoryImpl(FileScanner())

    private fun fileNode(path: String, isDirectory: Boolean = false) = FileNode(
        name = path.substringAfterLast('/'),
        path = path,
        sizeBytes = 0L,
        isDirectory = isDirectory
    )

    @Test
    fun `deleteNode existing temp file returns success`() = runTest {
        val file = tempFolder.newFile("repo_delete.txt")
        file.writeText("data")

        val result = repository.deleteNode(fileNode(file.absolutePath))

        assertTrue(result.isSuccess)
        assertFalse(file.exists())
    }

    @Test
    fun `deleteNode non-existent path returns failure`() = runTest {
        val fakePath = tempFolder.root.absolutePath + "/ghost.txt"

        val result = repository.deleteNode(fileNode(fakePath))

        assertTrue(result.isFailure)
    }
}
