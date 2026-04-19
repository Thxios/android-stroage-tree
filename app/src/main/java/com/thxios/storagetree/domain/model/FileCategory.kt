package com.thxios.storagetree.domain.model

enum class FileCategory {
    IMAGE, VIDEO, AUDIO, DOCUMENT, APK, ARCHIVE, OTHER;

    companion object {
        private val extensionMap: Map<String, FileCategory> = buildMap {
            listOf("jpg","jpeg","png","gif","webp","bmp","heic","heif").forEach { put(it, IMAGE) }
            listOf("mp4","mkv","avi","mov","wmv","flv","webm","3gp").forEach { put(it, VIDEO) }
            listOf("mp3","aac","flac","ogg","wav","m4a","opus").forEach { put(it, AUDIO) }
            listOf("pdf","doc","docx","xls","xlsx","ppt","pptx","txt","md").forEach { put(it, DOCUMENT) }
            listOf("apk","xapk","apks").forEach { put(it, APK) }
            listOf("zip","rar","7z","tar","gz","bz2").forEach { put(it, ARCHIVE) }
        }

        fun of(filename: String): FileCategory {
            val ext = filename.substringAfterLast('.', "").lowercase()
            return extensionMap[ext] ?: OTHER
        }
    }
}
