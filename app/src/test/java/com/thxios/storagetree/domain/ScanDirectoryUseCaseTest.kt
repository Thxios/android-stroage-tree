package com.thxios.storagetree.domain

import app.cash.turbine.test
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.repository.StorageRepository
import com.thxios.storagetree.domain.usecase.ScanDirectoryUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDirectoryUseCaseTest {
    private val repository = mockk<StorageRepository>()
    private val useCase = ScanDirectoryUseCase(repository)

    @Test
    fun `invoke delegates to repository scan`() = runTest {
        val fakePath = "/storage/emulated/0"
        every { repository.scan(fakePath) } returns flowOf(ScanState.Idle)

        useCase(fakePath).test {
            awaitItem()
            awaitComplete()
        }

        verify(exactly = 1) { repository.scan(fakePath) }
    }

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val fakePath = "/test"
        val fakeNode = com.thxios.storagetree.domain.model.FileNode(
            name = "test", path = fakePath, sizeBytes = 0L, isDirectory = true
        )
        every { repository.scan(fakePath) } returns flowOf(ScanState.Done(fakeNode))

        useCase(fakePath).test {
            val item = awaitItem()
            assertTrue(item is ScanState.Done)
            awaitComplete()
        }
    }
}
