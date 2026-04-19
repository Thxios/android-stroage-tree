package com.thxios.storagetree.domain

import com.thxios.storagetree.data.scanner.FileSizeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatterTest {
    private val formatter = FileSizeFormatter()

    @Test
    fun `formats 0 bytes`() {
        assertEquals("0 B", formatter.format(0L))
    }

    @Test
    fun `formats bytes below 1KB`() {
        assertEquals("1023 B", formatter.format(1023L))
    }

    @Test
    fun `formats exactly 1KB`() {
        assertEquals("1.0 KB", formatter.format(1024L))
    }

    @Test
    fun `formats 1_5 MB`() {
        assertEquals("1.5 MB", formatter.format(1024L * 1024 + 512 * 1024))
    }

    @Test
    fun `formats 2GB boundary`() {
        assertEquals("2.0 GB", formatter.format(2L * 1024 * 1024 * 1024))
    }
}
