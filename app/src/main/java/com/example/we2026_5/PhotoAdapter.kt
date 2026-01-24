package com.example.we2026_5

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PhotoAdapter(
    private var photoUrls: List<String>,
    private val onPhotoClick: (String) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    inner class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPhotoClick(photoUrls[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoUrl = photoUrls[position]
        Glide.with(holder.itemView.context)
            .load(photoUrl)
            .centerCrop()
            .placeholder(R.drawable.ic_launcher_background) // Platzhalter-Icon
            .into(holder.ivThumbnail)
    }

    override fun getItemCount(): Int = photoUrls.size

    fun updatePhotos(newUrls: List<String>) {
        photoUrls = newUrls
        notifyDataSetChanged()
    }
}
