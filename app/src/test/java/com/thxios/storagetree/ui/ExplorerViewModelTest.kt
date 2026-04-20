package com.thxios.storagetree.ui

import android.content.Context
import app.cash.turbine.test
import com.thxios.storagetree.data.storage.StorageRoot
import com.thxios.storagetree.data.storage.StorageVolumeHelper
import com.thxios.storagetree.domain.model.FileNode
import com.thxios.storagetree.domain.model.ScanState
import com.thxios.storagetree.domain.model.ViewMode
import com.thxios.storagetree.domain.usecase.CategorizeFilesUseCase
import com.thxios.storagetree.domain.usecase.DeleteNodeUseCase
import com.thxios.storagetree.domain.usecase.ScanDirectoryUseCase
import com.thxios.storagetree.ui.explorer.ExplorerViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExplorerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val scanUseCase = mockk<ScanDirectoryUseCase>()
    private val deleteUseCase = mockk<DeleteNodeUseCase>(relaxed = true)
    private val categorizeUseCase = CategorizeFilesUseCase()
    private val storageVolumeHelper = mockk<StorageVolumeHelper>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private lateinit var viewModel: ExplorerViewModel

    private val childA = FileNode(name = "a.txt", path = "/root/a.txt", sizeBytes = 300L, isDirectory = false)
    private val childB = FileNode(name = "b.txt", path = "/root/b.txt", sizeBytes = 100L, isDirectory = false)
    private val grandChild = FileNode(name = "c.txt", path = "/root/dir/c.txt", sizeBytes = 50L, isDirectory = false)
    private val subDir = FileNode(name = "dir", path = "/root/dir", sizeBytes = 50L, isDirectory = true, children = listOf(grandChild))
    private val rootNode = FileNode(
        name = "root",
        path = "/root",
        sizeBytes = 450L,
        isDirectory = true,
        children = listOf(childB, childA, subDir)
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { storageVolumeHelper.getAvailableRoots(any()) } returns emptyList()
        viewModel = ExplorerViewModel(scanUseCase, deleteUseCase, categorizeUseCase, storageVolumeHelper, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState scanState is Idle`() {
        assertEquals(ScanState.Idle, viewModel.uiState.value.scanState)
    }

    @Test
    fun `startScan emits Scanning then Done and uiState transitions accordingly`() = runTest {
        every { scanUseCase("/root") } returns flowOf(
            ScanState.Scanning("/root/subdir"),
            ScanState.Done(rootNode)
        )

        viewModel.uiState.test {
            awaitItem() // initial Idle state

            viewModel.startScan("/root")
            testDispatcher.scheduler.advanceUntilIdle()

            val scanning = awaitItem()
            assertTrue(scanning.isScanning)
            assertEquals("/root/subdir", scanning.scanningCurrentPath)

            val done = awaitItem()
            assertEquals(false, done.isScanning)
            assertTrue(done.scanState is ScanState.Done)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `after Done displayedChildren are sorted descending by sizeBytes`() = runTest {
        every { scanUseCase("/root") } returns flowOf(ScanState.Done(rootNode))

        viewModel.startScan("/root")
        testDispatcher.scheduler.advanceUntilIdle()

        val children = viewModel.uiState.value.displayedChildren
        assertEquals(listOf(childA, childB, subDir), children)
    }

    @Test
    fun `navigateTo sets displayedChildren to node children`() = runTest {
        every { scanUseCase("/root") } returns flowOf(ScanState.Done(rootNode))

        viewModel.startScan("/root")
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.navigateTo(subDir)

        val children = viewModel.uiState.value.displayedChildren
        assertEquals(listOf(grandChild), children)
    }

    @Test
    fun `navigateUp after navigateTo restores parent children`() = runTest {
        every { scanUseCase("/root") } returns flowOf(ScanState.Done(rootNode))

        viewModel.startScan("/root")
        testDispatcher.scheduler.advanceUntilIdle()

        val parentChildren = viewModel.uiState.value.displayedChildren

        viewModel.navigateTo(subDir)
        viewModel.navigateUp()

        assertEquals(parentChildren, viewModel.uiState.value.displayedChildren)
    }

    @Test
    fun `navigateUp_restoresCurrentPath`() = runTest {
        every { scanUseCase("/root") } returns flowOf(ScanState.Done(rootNode))

        viewModel.startScan("/root")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("/root", viewModel.uiState.value.currentPath)

        viewModel.navigateTo(subDir)
        assertEquals("/root/dir", viewModel.uiState.value.currentPath)

        viewModel.navigateUp()
        assertEquals("/root", viewModel.uiState.value.currentPath)
    }

    @Test
    fun `navigateUp at root with empty backstack is a no-op`() = runTest {
        every { scanUseCase("/root") } returns flowOf(ScanState.Done(rootNode))

        viewModel.startScan("/root")
        testDispatcher.scheduler.advanceUntilIdle()

        val stateBefore = viewModel.uiState.value

        viewModel.navigateUp()

        assertEquals(stateBefore, viewModel.uiState.value)
    }

    @Test
    fun `scanning_withRootNode_updatesDisplayedChildren`() = runTest {
        val partialRoot = FileNode(
            name = "root",
            path = "/root",
            sizeBytes = 300L,
            isDirectory = true,
            children = listOf(childA, childB)
        )
        every { scanUseCase("/root") } returns flowOf(
            ScanState.Scanning("/root/subdir", rootNode = partialRoot),
            ScanState.Done(rootNode)
        )

        viewModel.uiState.test {
            awaitItem() // initial state

            viewModel.startScan("/root")
            testDispatcher.scheduler.advanceUntilIdle()

            val scanning = awaitItem()
            assertTrue(scanning.isScanning)
            // Should show partial children sorted descending
            assertEquals(listOf(childA, childB), scanning.displayedChildren)
            assertEquals("/root", scanning.currentPath)

            val done = awaitItem()
            assertEquals(false, done.isScanning)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `scan emits Error and uiState error is non-null`() = runTest {
        every { scanUseCase("/root") } returns flowOf(ScanState.Error("Permission denied"))

        viewModel.startScan("/root")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals("Permission denied", viewModel.uiState.value.error)
    }

    @Test
    fun `initial viewMode is LIST`() {
        assertEquals(ViewMode.LIST, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `toggleViewMode changes viewMode from LIST to TREEMAP`() {
        viewModel.toggleViewMode()
        assertEquals(ViewMode.TREEMAP, viewModel.uiState.value.viewMode)
    }

    @Test
    fun `toggleViewMode twice returns viewMode to LIST`() {
        viewModel.toggleViewMode()
        viewModel.toggleViewMode()
        assertEquals(ViewMode.LIST, viewModel.uiState.value.viewMode)
    }
}
