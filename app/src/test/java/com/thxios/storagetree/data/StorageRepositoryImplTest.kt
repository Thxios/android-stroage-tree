package com.thxios.storagetree.data

import app.cash.turbine.test
import com.thxios.storagetree.data.repository.StorageRepositoryImpl
import com.thxios.storagetree.data.scanner.FileScanner
import com.thxios.storagetree.domain.model.ScanState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class StorageRepositoryImplTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val repository = StorageRepositoryImpl(FileScanner())

    @Test
    fun `scan real directory emits Done`() = runTest {
        val dir = tempFolder.newFolder("test")
        dir.resolve("file.txt").writeBytes(ByteArray(50))

        repository.scan(dir.absolutePath).test {
            var gotDone = false
            while (true) {
                val item = awaitItem()
                if (item is ScanState.Done) {
                    gotDone = true
                    cancelAndConsumeRemainingEvents()
                    break
                }
            }
            assertTrue(gotDone)
        }
    }

    @Test
    fun `scan nonexistent path emits Error`() = runTest {
        repository.scan("/nonexistent/path/xyz123").test {
            var gotError = false
            while (true) {
                val item = awaitItem()
                if (item is ScanState.Error) {
                    gotError = true
                    cancelAndConsumeRemainingEvents()
                    break
                }
                if (item is ScanState.Done) {
                    cancelAndConsumeRemainingEvents()
                    break
                }
            }
            assertTrue(gotError)
        }
    }
}
