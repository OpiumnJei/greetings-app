package com.greetingsapp.mobile.adapter

import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.databinding.ItemImageBinding
import com.greetingsapp.mobile.model.ImageModel

// Esta clase interna "retiene" las vistas de una tarjeta para no buscarlas todo el tiempo gracias al viewBinding
class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(image: ImageModel) {

        //URL temporal que representa nueva maquina usada mientras desarrollamos, ya que por defecto si usuramos
        // localhost estariamos apuntando al emulador/dispositivo android no nuestra pc
        val fixedUrl = image.imageUrl.replace("localhost", "10.0.2.2")

        // 1. Cargar la imagen usando Coil
        // La extensión .load viene de la librería Coil
        binding.imageViewItem.load(fixedUrl) {
            crossfade(true) // Efecto de desvanecimiento suave al aparecer
            placeholder(R.drawable.ic_launcher_background) // Muestra esto mientras carga
            error(R.drawable.ic_launcher_background) // Muestra esto si falla (ej. sin internet)
        }
    }

}