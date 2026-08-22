package com.fa1roxx.filemanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.fa1roxx.filemanager.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FileAdapter

    private val rootDir: File get() = Environment.getExternalStorageDirectory()
    private var currentDir: File = Environment.getExternalStorageDirectory()

    private val androidDataDir get() = File(rootDir, "Android/data")
    private val androidObbDir get() = File(rootDir, "Android/obb")
    private val androidMediaDir get() = File(rootDir, "Android/media")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = FileAdapter(emptyList()) { item -> onItemClick(item) }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnData.setOnClickListener { openQuickFolder(androidDataDir, "Android/data") }
        binding.btnObb.setOnClickListener { openQuickFolder(androidObbDir, "Android/obb") }
        binding.btnMedia.setOnClickListener { openQuickFolder(androidMediaDir, "Android/media") }
        binding.btnShizuku.setOnClickListener { requestShizuku() }
        binding.btnHome.setOnClickListener { navigateTo(rootDir) }

        ensureStoragePermission()
        navigateTo(currentDir)
    }

    private fun ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                101
            )
        }
    }

    private fun requestShizuku() {
        if (!ShizukuHelper.isAvailable()) {
            Toast.makeText(this, "Shizuku не запущен. Установите и запустите приложение Shizuku.", Toast.LENGTH_LONG).show()
            return
        }
        if (ShizukuHelper.hasPermission()) {
            Toast.makeText(this, "Доступ Shizuku уже предоставлен ✓", Toast.LENGTH_SHORT).show()
        } else {
            ShizukuHelper.requestPermission()
        }
    }

    private fun openQuickFolder(folder: File, label: String) {
        if (folder.canRead() && (folder.listFiles() != null)) {
            navigateTo(folder)
        } else if (ShizukuHelper.hasPermission()) {
            val names = ShizukuHelper.listDir(folder.absolutePath)
            if (names.isEmpty()) {
                Toast.makeText(this, "$label пуста или недоступна", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "$label открыта через Shizuku (${names.size} эл.)", Toast.LENGTH_SHORT).show()
                navigateTo(folder)
            }
        } else {
            Toast.makeText(
                this,
                "$label защищена системой Android. Разрешите доступ через Shizuku (кнопка вверху).",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun navigateTo(dir: File) {
        currentDir = dir
        supportActionBar?.title = if (dir == rootDir) "FA1ROXX FILE MANAGER" else dir.name

        val files = dir.listFiles()?.toList() ?: emptyList()
        val items = files
            .map { FileItem(it) }
            .sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })

        adapter.update(items)
        binding.emptyState.visibility = if (items.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun onItemClick(item: FileItem) {
        if (item.isDirectory) {
            navigateTo(item.file)
            return
        }
        when (FileCategory.of(item)) {
            FileCategory.APK -> FileOpener.installApk(this, item.file)
            else -> FileOpener.openFile(this, item.file)
        }
    }

    override fun onBackPressed() {
        if (currentDir != rootDir && currentDir.parentFile != null) {
            navigateTo(currentDir.parentFile!!)
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            navigateTo(currentDir)
        }
    }
}
