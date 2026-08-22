package com.fa1roxx.filemanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class FileAdapter(
    private var items: List<FileItem>,
    private val onClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardRoot)
        val icon: android.widget.ImageView = view.findViewById(R.id.imgIcon)
        val name: android.widget.TextView = view.findViewById(R.id.txtName)
        val meta: android.widget.TextView = view.findViewById(R.id.txtMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.meta.text = if (item.isDirectory) "Папка" else item.readableSize()

        val iconRes = when (FileCategory.of(item)) {
            FileCategory.FOLDER -> R.drawable.ic_folder
            FileCategory.IMAGE -> R.drawable.ic_image
            FileCategory.VIDEO -> R.drawable.ic_video
            FileCategory.AUDIO -> R.drawable.ic_audio
            FileCategory.APK -> R.drawable.ic_apk
            FileCategory.ARCHIVE -> R.drawable.ic_archive
            FileCategory.DOCUMENT -> R.drawable.ic_document
            FileCategory.OTHER -> R.drawable.ic_file
        }
        holder.icon.setImageResource(iconRes)
        holder.card.setOnClickListener { onClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun update(newItems: List<FileItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
