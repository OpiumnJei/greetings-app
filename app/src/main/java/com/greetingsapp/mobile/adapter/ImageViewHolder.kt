package com.greetingsapp.mobile.adapter

import androidx.recyclerview.widget.RecyclerView
import com.greetingsapp.mobile.databinding.ItemImageBinding
import com.greetingsapp.mobile.model.ImageModel

// Esta clase interna "retiene" las vistas de una tarjeta para no buscarlas todo el tiempo
class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(image: ImageModel) {
        // AQUÍ OCURRE LA MAGIA: Conectamos los datos con la vista
        // Por ahora solo usaremos un placeholder, en la siguiente misión cargaremos la URL real
        // binding.imageViewItem.load(image.imageUrl) <--- Esto lo haremos con Coil/Glide pronto
    }

}