package com.example.dualplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class PlayerAdapter(private val listener: Listener) :
    ListAdapter<DocumentFile, PlayerAdapter.VH>(Diff()) {

    interface Listener { fun onAudioClick(documentFile: DocumentFile) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_audio, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = getItem(position)
        holder.title.text = file.name ?: "Unknown"
        holder.subtitle.text = file.type ?: ""
        holder.itemView.setOnClickListener { listener.onAudioClick(file) }
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.audioTitle)
        val subtitle: TextView = v.findViewById(R.id.audioSubtitle)
    }

    class Diff : DiffUtil.ItemCallback<DocumentFile>() {
        override fun areItemsTheSame(old: DocumentFile, new: DocumentFile) = old.uri == new.uri
        override fun areContentsTheSame(old: DocumentFile, new: DocumentFile) = old.uri == new.uri
    }
}
