package com.fa1roxx.filemanager

import java.io.File

data class FileItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val viaShizuku: Boolean = false
) {
    val file: File get() = File(path)

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

    companion object {
        fun fromFile(file: File): FileItem = FileItem(
            path = file.absolutePath,
            name = file.name,
            isDirectory = file.isDirectory,
            sizeBytes = if (file.isFile) file.length() else 0L,
            viaShizuku = false
        )
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
