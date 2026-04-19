package com.thxios.storagetree.domain

import com.thxios.storagetree.domain.model.FileCategory
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.usecase.CategorizeFilesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategorizeFilesUseCaseTest {

    private lateinit var useCase: CategorizeFilesUseCase

    @Before
    fun setUp() {
        useCase = CategorizeFilesUseCase()
    }

    @Test
    fun `single image leaf node returns map with IMAGE and correct size`() {
        val node = FileNode(name = "photo.jpg", path = "/photo.jpg", sizeBytes = 1000L, isDirectory = false)
        val result = useCase(node)
        assertEquals(1000L, result[FileCategory.IMAGE])
    }

    @Test
    fun `mixed files return correct per-category sums`() {
        val root = FileNode(
            name = "root", path = "/root", sizeBytes = 3000L, isDirectory = true,
            children = listOf(
                FileNode(name = "image.jpg", path = "/root/image.jpg", sizeBytes = 1000L, isDirectory = false),
                FileNode(name = "clip.mp4", path = "/root/clip.mp4", sizeBytes = 2000L, isDirectory = false),
                FileNode(name = "random.xyz", path = "/root/random.xyz", sizeBytes = 500L, isDirectory = false)
            )
        )
        val result = useCase(root)
        assertEquals(1000L, result[FileCategory.IMAGE])
        assertEquals(2000L, result[FileCategory.VIDEO])
        assertEquals(500L, result[FileCategory.OTHER])
    }

    @Test
    fun `unknown extension is counted under OTHER`() {
        val node = FileNode(name = "weird.abc123", path = "/weird.abc123", sizeBytes = 300L, isDirectory = false)
        val result = useCase(node)
        assertEquals(300L, result[FileCategory.OTHER])
    }

    @Test
    fun `nested directories are recursively collected`() {
        val root = FileNode(
            name = "root", path = "/root", sizeBytes = 5000L, isDirectory = true,
            children = listOf(
                FileNode(
                    name = "subdir", path = "/root/subdir", sizeBytes = 3000L, isDirectory = true,
                    children = listOf(
                        FileNode(name = "deep.png", path = "/root/subdir/deep.png", sizeBytes = 2000L, isDirectory = false)
                    )
                ),
                FileNode(name = "top.mp3", path = "/root/top.mp3", sizeBytes = 1500L, isDirectory = false)
            )
        )
        val result = useCase(root)
        assertEquals(2000L, result[FileCategory.IMAGE])
        assertEquals(1500L, result[FileCategory.AUDIO])
    }

    @Test
    fun `empty node list returns empty map`() {
        val root = FileNode(name = "root", path = "/root", sizeBytes = 0L, isDirectory = true, children = emptyList())
        val result = useCase(root)
        assertTrue(result.isEmpty())
    }
}
