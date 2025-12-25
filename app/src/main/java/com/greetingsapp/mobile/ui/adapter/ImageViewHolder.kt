package com.greetingsapp.mobile.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.databinding.ItemImageBinding
import com.greetingsapp.mobile.data.model.ImageModel

// Esta clase interna "retiene" las vistas de una tarjeta para no buscarlas todo el tiempo gracias al viewBinding
class ImageViewHolder(private val binding: ItemImageBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(image: ImageModel, onImageSelected: (ImageModel) -> Unit)//onImageSelected recibe un objeto del tipo ImageModel
    {
        // url de la imagen
        val imageUrl = image.imageUrl

        // 1. Cargar la imagen usando Coil
        // La extensión .load viene de la librería Coil
        binding.imageViewItem.load(imageUrl) {
            crossfade(true) // Efecto de desvanecimiento suave al aparecer
            placeholder(R.drawable.ic_launcher_background) // Muestra esto mientras carga
            error(R.drawable.ic_launcher_background) // Muestra esto si falla (ej. sin internet)
        }

        // al hacer click sobre una imagen se llama a este metodo
        binding.root.setOnClickListener { //binding.root - indica que estamos accediendo a TOD0 el componente padre(CardView)
            onImageSelected(image)
        }
    }

}

