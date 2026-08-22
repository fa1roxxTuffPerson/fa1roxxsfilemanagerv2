package com.fa1roxx.filemanager

import android.content.Context
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.reflect.Method

object ShizukuHelper {

    const val REQUEST_CODE = 7726

    fun isAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (e: Throwable) {
        false
    }

    fun hasPermission(): Boolean {
        if (!isAvailable()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission() {
        if (!isAvailable()) return
        if (Shizuku.isPreV11()) return
        Shizuku.requestPermission(REQUEST_CODE)
    }

    private fun newProcessReflect(cmd: Array<String>, env: Array<String>?, dir: String?): Process? {
        return try {
            val method: Method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(null, cmd, env, dir) as? Process
        } catch (e: Throwable) {
            null
        }
    }

    fun runShellCommand(command: String): String {
        if (!hasPermission()) return ""
        return try {
            val process = newProcessReflect(arrayOf("sh", "-c", command), null, null)
                ?: return ""
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            output
        } catch (e: Throwable) {
            ""
        }
    }

    /** Простой список имён файлов/папок (для совместимости с вызовом listDir). */
    fun listDir(path: String): List<String> {
        return listDirDetailed(path).map { it.first }
    }

    /** Список (имя, isDirectory, размер) для указанного пути через shell (обходит scoped storage). */
    fun listDirDetailed(path: String): List<Triple<String, Boolean, Long>> {
        val safePath = path.replace("\"", "\\\"")
        val command = "for f in \"$safePath\"/*; do [ -e \"\$f\" ] || continue; " +
            "n=\$(basename \"\$f\"); " +
            "if [ -d \"\$f\" ] ; then echo \"D|0|\$n\"; " +
            "else sz=\$(stat -c%s \"\$f\" 2>/dev/null || echo 0); echo \"F|\$sz|\$n\"; fi; done"
        val output = runShellCommand(command)
        return output.lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val parts = line.split("|", limit = 3)
            if (parts.size == 3) {
                val isDir = parts[0] == "D"
                val size = parts[1].toLongOrNull() ?: 0L
                Triple(parts[2], isDir, size)
            } else null
        }
    }

    fun mkdirViaShell(path: String): Boolean {
        val result = runShellCommand("mkdir -p \"$path\" && echo OK")
        return result.contains("OK")
    }

    fun touchViaShell(path: String): Boolean {
        val result = runShellCommand("touch \"$path\" && echo OK")
        return result.contains("OK")
    }

    fun copyViaShell(sourcePath: String, destPath: String): Boolean {
        val result = runShellCommand("cp -r \"$sourcePath\" \"$destPath\" && echo OK")
        return result.contains("OK")
    }

    fun moveViaShell(sourcePath: String, destPath: String): Boolean {
        val result = runShellCommand("mv \"$sourcePath\" \"$destPath\" && echo OK")
        return result.contains("OK")
    }

    fun deleteViaShell(path: String): Boolean {
        val result = runShellCommand("rm -rf \"$path\" && echo OK")
        return result.contains("OK")
    }

    /** Копирует файл из защищённой папки в кэш приложения, чтобы открыть его через FileProvider. */
    fun copyToAppCache(context: Context, sourcePath: String, fileName: String): File? {
        val destFile = File(context.cacheDir, "shizuku_view_$fileName")
        val ok = copyViaShell(sourcePath, destFile.absolutePath)
        return if (ok && destFile.exists()) destFile else null
    }
}
