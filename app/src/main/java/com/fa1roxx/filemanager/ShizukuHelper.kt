package com.fa1roxx.filemanager

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

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

    fun runShellCommand(command: String): String {
        if (!hasPermission()) return "Нет прав Shizuku"
        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            process.waitFor()
            output
        } catch (e: Throwable) {
            "Ошибка выполнения через Shizuku: ${e.message}"
        }
    }

    fun listDir(path: String): List<String> {
        val raw = runShellCommand("ls -1 \"$path\"")
        return raw.lines().filter { it.isNotBlank() }
    }

    fun copyViaShell(sourcePath: String, destPath: String): Boolean {
        val result = runShellCommand("cp -r \"$sourcePath\" \"$destPath\" && echo OK")
        return result.contains("OK")
    }

    fun deleteViaShell(path: String): Boolean {
        val result = runShellCommand("rm -rf \"$path\" && echo OK")
        return result.contains("OK")
    }
}
