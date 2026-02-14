package com.greetingsapp.mobile.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.databinding.ItemImageBinding
import com.greetingsapp.mobile.data.model.ImageModel

// Esta clase interna "retiene" las vistas de una tarjeta para no buscarlas todo el tiempo gracias al viewBinding
class ImageViewHolder(private val binding: ItemImageBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(image: ImageModel, onImageSelected: (ImageModel) -> Unit)
    {
        // 1. Cargar la imagen usando Coil
        binding.imageViewItem.load(image.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_broken_image)
        }

        // Al hacer click sobre una imagen se llama a este método
        binding.root.setOnClickListener {
            onImageSelected(image)
        }
    }

}

