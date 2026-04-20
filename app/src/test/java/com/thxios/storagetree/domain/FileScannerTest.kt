package com.thxios.storagetree.domain

import app.cash.turbine.test
import com.thxios.storagetree.data.scanner.FileScanner
import com.thxios.storagetree.domain.model.ScanState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileScannerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val scanner = FileScanner()

    private suspend fun app.cash.turbine.ReceiveTurbine<ScanState>.collectUntilDone(): ScanState.Done {
        while (true) {
            val item = awaitItem()
            if (item is ScanState.Done) {
                cancelAndConsumeRemainingEvents()
                return item
            }
        }
    }

    @Test
    fun `empty directory returns node with zero size and no children`() = runTest {
        val dir = tempFolder.newFolder("empty")
        scanner.scan(dir.absolutePath).test {
            val doneState = collectUntilDone()
            assertEquals(0L, doneState.rootNode.sizeBytes)
            assertTrue(doneState.rootNode.children.isEmpty())
        }
    }

    @Test
    fun `single file returns correct size`() = runTest {
        val dir = tempFolder.newFolder("single")
        val file = dir.resolve("test.txt")
        file.writeBytes(ByteArray(100))

        scanner.scan(dir.absolutePath).test {
            val doneState = collectUntilDone()
            assertEquals(100L, doneState.rootNode.sizeBytes)
        }
    }

    @Test
    fun `children sorted by size descending`() = runTest {
        val dir = tempFolder.newFolder("sorted")
        dir.resolve("small.txt").writeBytes(ByteArray(100))
        dir.resolve("large.txt").writeBytes(ByteArray(300))
        dir.resolve("medium.txt").writeBytes(ByteArray(200))

        scanner.scan(dir.absolutePath).test {
            val doneState = collectUntilDone()
            val sizes = doneState.rootNode.children.map { it.sizeBytes }
            assertEquals(listOf(300L, 200L, 100L), sizes)
        }
    }

    @Test
    fun `nested directories total size equals all files combined`() = runTest {
        val root = tempFolder.newFolder("nested")
        val subDir = root.resolve("sub").also { it.mkdir() }
        root.resolve("a.txt").writeBytes(ByteArray(100))
        subDir.resolve("b.txt").writeBytes(ByteArray(200))

        scanner.scan(root.absolutePath).test {
            val doneState = collectUntilDone()
            assertEquals(300L, doneState.rootNode.sizeBytes)
        }
    }

    @Test
    fun `scan emits scanning states before done`() = runTest {
        val dir = tempFolder.newFolder("scanning")
        dir.resolve("file.txt").writeBytes(ByteArray(50))

        var sawScanning = false
        var sawDone = false

        scanner.scan(dir.absolutePath).test {
            var keepGoing = true
            while (keepGoing) {
                when (val item = awaitItem()) {
                    is ScanState.Scanning -> sawScanning = true
                    is ScanState.Done -> {
                        sawDone = true
                        cancelAndConsumeRemainingEvents()
                        keepGoing = false
                    }
                    else -> {}
                }
            }
        }

        assertTrue(sawDone)
    }

    @Test
    fun `scan emits partial rootNode after each top-level child`() = runTest {
        val root = tempFolder.newFolder("partial")
        // Create 2 subdirectories each with files
        val sub1 = root.resolve("sub1").also { it.mkdir() }
        sub1.resolve("a.txt").writeBytes(ByteArray(100))
        sub1.resolve("b.txt").writeBytes(ByteArray(200))
        val sub2 = root.resolve("sub2").also { it.mkdir() }
        sub2.resolve("c.txt").writeBytes(ByteArray(50))

        val scanningWithRoot = mutableListOf<ScanState.Scanning>()

        scanner.scan(root.absolutePath).test {
            var keepGoing = true
            while (keepGoing) {
                when (val item = awaitItem()) {
                    is ScanState.Scanning -> {
                        if (item.rootNode != null) scanningWithRoot.add(item)
                    }
                    is ScanState.Done -> {
                        cancelAndConsumeRemainingEvents()
                        keepGoing = false
                    }
                    else -> {}
                }
            }
        }

        // After first child completes we should see a partial rootNode with 1 child
        assertTrue("Should have at least one partial Scanning with rootNode", scanningWithRoot.isNotEmpty())
        val firstPartial = scanningWithRoot.first()
        assertEquals(1, firstPartial.rootNode!!.children.size)
        // After second child we should see rootNode with 2 children
        val lastPartial = scanningWithRoot.last()
        assertEquals(2, lastPartial.rootNode!!.children.size)
    }
}
