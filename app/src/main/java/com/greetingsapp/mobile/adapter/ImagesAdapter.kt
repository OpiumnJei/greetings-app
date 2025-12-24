package com.greetingsapp.mobile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.greetingsapp.mobile.databinding.ItemImageBinding
import com.greetingsapp.mobile.model.ImageModel

class ImagesAdapter(private val onImageSelected: (ImageModel) -> Unit) : ListAdapter<ImageModel, ImageViewHolder>(DiffCallback) {


    // Crea la "cascara" visual (el ViewHolder) inflando el XML
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    // Rellena la cáscara con los datos de una posición específica
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = getItem(position)
        holder.bind(image, onImageSelected)
    }

    // Esto ayuda al adaptador a saber qué cambió en la lista para actualizar solo lo necesario (eficiencia pura)
    companion object DiffCallback : DiffUtil.ItemCallback<ImageModel>() {
        override fun areItemsTheSame(oldItem: ImageModel, newItem: ImageModel): Boolean {
            return oldItem.imageId == newItem.imageId
        }

        override fun areContentsTheSame(oldItem: ImageModel, newItem: ImageModel): Boolean {
            return oldItem == newItem
        }
    }
}