package com.fa1roxx.filemanager

import java.io.File

data class FileItem(
    val file: File,
    val name: String = file.name,
    val isDirectory: Boolean = file.isDirectory,
    val sizeBytes: Long = if (file.isFile) file.length() else 0L,
    val lastModified: Long = file.lastModified()
) {
    val extension: String
        get() = name.substringAfterLast('.', "").lowercase()

    fun readableSize(): String {
        if (isDirectory) return ""
        val kb = 1024.0
        val mb = kb * 1024
        val gb = mb * 1024
        return when {
            sizeBytes >= gb -> String.format("%.2f ГБ", sizeBytes / gb)
            sizeBytes >= mb -> String.format("%.2f МБ", sizeBytes / mb)
            sizeBytes >= kb -> String.format("%.2f КБ", sizeBytes / kb)
            else -> "$sizeBytes Б"
        }
    }
}

enum class FileCategory {
    FOLDER, IMAGE, VIDEO, AUDIO, APK, ARCHIVE, DOCUMENT, OTHER;

    companion object {
        fun of(item: FileItem): FileCategory {
            if (item.isDirectory) return FOLDER
            return when (item.extension) {
                "jpg", "jpeg", "png", "webp", "bmp", "gif", "heic" -> IMAGE
                "mp4", "mkv", "webm", "3gp", "mov", "avi" -> VIDEO
                "mp3", "wav", "flac", "ogg", "m4a", "aac" -> AUDIO
                "apk" -> APK
                "zip", "rar", "7z", "tar", "gz" -> ARCHIVE
                "pdf", "txt", "doc", "docx", "xls", "xlsx" -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
