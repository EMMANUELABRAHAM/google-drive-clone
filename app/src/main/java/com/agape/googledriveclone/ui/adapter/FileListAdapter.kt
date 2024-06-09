package com.agape.googledriveclone.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agape.googledriveclone.R
import com.agape.googledriveclone.model.GoogleDriveFile

class FileListAdapter(
private val files: List<GoogleDriveFile>,
private val onItemClick: (GoogleDriveFile) -> Unit,
private val onDownloadClick: (GoogleDriveFile) -> Unit
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {

    inner class FileViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fileName: TextView = view.findViewById(R.id.fileNameTextView)
        val downloadButton: ImageButton = view.findViewById(R.id.downloadButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val file = files[position]
        holder.fileName.text = file.name
        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
        holder.downloadButton.setOnClickListener {
            onDownloadClick(file)
        }
    }

    override fun getItemCount() = files.size
}
