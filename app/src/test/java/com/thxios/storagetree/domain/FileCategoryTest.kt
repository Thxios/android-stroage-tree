package com.thxios.storagetree.domain

import com.thxios.storagetree.domain.model.FileCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class FileCategoryTest {

    @Test
    fun `jpg maps to IMAGE`() {
        assertEquals(FileCategory.IMAGE, FileCategory.of("photo.JPG"))
    }

    @Test
    fun `mp4 maps to VIDEO`() {
        assertEquals(FileCategory.VIDEO, FileCategory.of("video.mp4"))
    }

    @Test
    fun `mp3 maps to AUDIO`() {
        assertEquals(FileCategory.AUDIO, FileCategory.of("song.MP3"))
    }

    @Test
    fun `pdf maps to DOCUMENT`() {
        assertEquals(FileCategory.DOCUMENT, FileCategory.of("doc.pdf"))
    }

    @Test
    fun `apk maps to APK`() {
        assertEquals(FileCategory.APK, FileCategory.of("app.apk"))
    }

    @Test
    fun `zip maps to ARCHIVE`() {
        assertEquals(FileCategory.ARCHIVE, FileCategory.of("archive.zip"))
    }

    @Test
    fun `no extension maps to OTHER`() {
        assertEquals(FileCategory.OTHER, FileCategory.of("noextension"))
    }

    @Test
    fun `unknown extension maps to OTHER`() {
        assertEquals(FileCategory.OTHER, FileCategory.of("file.UNKNOWN"))
    }

    @Test
    fun `case insensitive - PNG maps to IMAGE`() {
        assertEquals(FileCategory.IMAGE, FileCategory.of("Photo.PNG"))
    }
}
