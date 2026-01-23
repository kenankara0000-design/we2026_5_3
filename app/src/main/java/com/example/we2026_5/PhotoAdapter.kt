package com.example.we2026_5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.we2026_5.R // Dieser Import verbindet den Code mit deinen XML-Dateien
import java.io.File

class PhotoAdapter(private val photos: List<File>) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        // Falls R.layout.item_photo rot bleibt, bitte Schritt 4 (unten) ausführen
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoFile = photos[position]
        Glide.with(holder.itemView.context)
            .load(photoFile)
            .into(holder.ivThumbnail)
    }

    override fun getItemCount(): Int = photos.size
}