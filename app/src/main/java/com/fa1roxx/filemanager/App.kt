package com.fa1roxx.filemanager

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val log = buildString {
                    appendLine("FA1ROXX FILE MANAGER - CRASH LOG")
                    appendLine(java.util.Date().toString())
                    appendLine()
                    appendLine(throwable.stackTraceToString())
                }
                writeCrashLog(applicationContext, log)
            } catch (e: Throwable) {
                // игнорируем ошибки самого логгера
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun writeCrashLog(context: Context, text: String) {
        val fileName = "fa1roxx_crash_${System.currentTimeMillis()}.txt"
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { os -> os.write(text.toByteArray()) }
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, fileName)
                FileOutputStream(file).use { it.write(text.toByteArray()) }
            }
        } catch (e: Throwable) {
            // если и это не сработало, ничего не поделать без адб пиздец будет от фаирокса не ии и вообще какого хуя ты смотришь этот код пидар
        }
    }
}
