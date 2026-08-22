package com.fa1roxx.filemanager

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
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

    private var currentPath: String = ""
    private var currentIsShizuku: Boolean = false

    private var clipboardItem: FileItem? = null
    private var clipboardIsCut: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        adapter = FileAdapter(
            emptyList(),
            onClick = { item -> onItemClick(item) },
            onLongClick = { item -> onItemLongClick(item) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnData.setOnClickListener {
            navigateTo(File(rootDir, "Android/data").absolutePath, ShizukuHelper.hasPermission())
        }
        binding.btnObb.setOnClickListener {
            navigateTo(File(rootDir, "Android/obb").absolutePath, ShizukuHelper.hasPermission())
        }
        binding.btnMedia.setOnClickListener {
            navigateTo(File(rootDir, "Android/media").absolutePath, false)
        }
        binding.btnShizuku.setOnClickListener { requestShizuku() }
        binding.btnHome.setOnClickListener { navigateTo(rootDir.absolutePath, false) }

        ensureStoragePermission()
        navigateTo(rootDir.absolutePath, false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_new_folder -> { createNewFolder(); true }
            R.id.action_new_file -> { createNewFile(); true }
            R.id.action_paste -> { pasteClipboard(); true }
            else -> super.onOptionsItemSelected(item)
        }
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

    private fun navigateTo(path: String, viaShizuku: Boolean) {
        var useShizuku = viaShizuku
        var items: List<FileItem> = emptyList()

        if (!useShizuku) {
            val dirFile = File(path)
            val files = dirFile.listFiles()
            if (files != null) {
                items = files.map { FileItem.fromFile(it) }
            } else if (ShizukuHelper.hasPermission()) {
                useShizuku = true
            } else {
                Toast.makeText(
                    this,
                    "Эта папка защищена системой Android. Разрешите доступ через Shizuku (кнопка со щитом).",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (useShizuku) {
            val entries = ShizukuHelper.listDirDetailed(path)
            items = entries.map { (name, isDir, size) ->
                FileItem(path = "$path/$name", name = name, isDirectory = isDir, sizeBytes = size, viaShizuku = true)
            }
        }

        currentPath = path
        currentIsShizuku = useShizuku

        supportActionBar?.title = if (path == rootDir.absolutePath) "FA1ROXX FILE MANAGER" else File(path).name
        val sorted = items.sortedWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name.lowercase() })
        adapter.update(sorted)
        binding.emptyState.visibility = if (sorted.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun onItemClick(item: FileItem) {
        if (item.isDirectory) {
            navigateTo(item.path, item.viaShizuku)
            return
        }
        if (item.viaShizuku) {
            val cached = ShizukuHelper.copyToAppCache(this, item.path, item.name)
            if (cached == null) {
                Toast.makeText(this, "Не удалось открыть файл через Shizuku", Toast.LENGTH_SHORT).show()
                return
            }
            if (FileCategory.of(item) == FileCategory.APK) FileOpener.installApk(this, cached)
            else FileOpener.openFile(this, cached)
        } else {
            when (FileCategory.of(item)) {
                FileCategory.APK -> FileOpener.installApk(this, item.file)
                else -> FileOpener.openFile(this, item.file)
            }
        }
    }

    private fun onItemLongClick(item: FileItem) {
        val options = arrayOf("Копировать", "Вырезать", "Удалить", "Отмена")
        AlertDialog.Builder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { clipboardItem = item; clipboardIsCut = false; Toast.makeText(this, "Скопировано: ${item.name}", Toast.LENGTH_SHORT).show() }
                    1 -> { clipboardItem = item; clipboardIsCut = true; Toast.makeText(this, "Вырезано: ${item.name}", Toast.LENGTH_SHORT).show() }
                    2 -> confirmDelete(item)
                }
            }
            .show()
    }

    private fun confirmDelete(item: FileItem) {
        AlertDialog.Builder(this)
            .setTitle("Удалить \"${item.name}\"?")
            .setMessage("Это действие нельзя отменить")
            .setPositiveButton("Удалить") { _, _ ->
                val ok = if (item.viaShizuku || ShizukuHelper.hasPermission()) ShizukuHelper.deleteViaShell(item.path)
                else item.file.deleteRecursively()
                if (ok) {
                    Toast.makeText(this, "Удалено", Toast.LENGTH_SHORT).show()
                    navigateTo(currentPath, currentIsShizuku)
                } else {
                    Toast.makeText(this, "Не удалось удалить", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun pasteClipboard() {
        val clip = clipboardItem
        if (clip == null) {
            Toast.makeText(this, "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
            return
        }
        val destPath = "$currentPath/${clip.name}"
        val useShizuku = clip.viaShizuku || currentIsShizuku || ShizukuHelper.hasPermission()

        val ok = if (useShizuku) {
            if (clipboardIsCut) ShizukuHelper.moveViaShell(clip.path, destPath) else ShizukuHelper.copyViaShell(clip.path, destPath)
        } else {
            try {
                if (clipboardIsCut) clip.file.renameTo(File(destPath)) else clip.file.copyRecursively(File(destPath), overwrite = true)
                true
            } catch (e: Exception) { false }
        }

        if (ok) {
            Toast.makeText(this, "Вставлено", Toast.LENGTH_SHORT).show()
            if (clipboardIsCut) clipboardItem = null
            navigateTo(currentPath, currentIsShizuku)
        } else {
            Toast.makeText(this, "Не удалось вставить", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNewFolder() {
        val input = EditText(this)
        input.hint = "Имя папки"
        AlertDialog.Builder(this)
            .setTitle("Новая папка")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val newPath = "$currentPath/$name"
                val ok = if (currentIsShizuku || ShizukuHelper.hasPermission()) ShizukuHelper.mkdirViaShell(newPath)
                else File(newPath).mkdirs()
                if (ok) navigateTo(currentPath, currentIsShizuku)
                else Toast.makeText(this, "Не удалось создать папку", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createNewFile() {
        val input = EditText(this)
        input.hint = "Имя файла (например notes.txt)"
        AlertDialog.Builder(this)
            .setTitle("Новый файл")
            .setView(input)
            .setPositiveButton("Создать") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                val newPath = "$currentPath/$name"
                val ok = if (currentIsShizuku || ShizukuHelper.hasPermission()) ShizukuHelper.touchViaShell(newPath)
                else try { File(newPath).createNewFile() } catch (e: Exception) { false }
                if (ok) navigateTo(currentPath, currentIsShizuku)
                else Toast.makeText(this, "Не удалось создать файл", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onBackPressed() {
        val parent = File(currentPath).parent
        if (currentPath != rootDir.absolutePath && parent != null) {
            navigateTo(parent, false)
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            navigateTo(currentPath, currentIsShizuku)
        }
    }
}
