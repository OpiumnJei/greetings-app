package com.greetingsapp.mobile.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import com.greetingsapp.mobile.R
import com.greetingsapp.mobile.databinding.ItemThemeBinding
import com.greetingsapp.mobile.data.model.ThemeModel

class ThemeViewHolder(private val binding: ItemThemeBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(theme: ThemeModel, isSelected: Boolean, onThemeSelected: (ThemeModel) -> Unit) { //onThemeSelected recibe un objeto del tipo The
        binding.textThemeName.text = theme.themeName

        // ⭐ LÓGICA VISUAL: Cambiar color según selección
        if (isSelected) {
            // Ejemplo: Fondo Morado, Texto Blanco
            binding.cardTheme.setCardBackgroundColor(binding.root.context.getColor(R.color.orange))
            binding.textThemeName.setTextColor(binding.root.context.getColor(android.R.color.white))
        } else {
            // Estado Normal: Fondo Blanco, Texto Negro
            binding.cardTheme.setCardBackgroundColor(binding.root.context.getColor(android.R.color.white))
            binding.textThemeName.setTextColor(binding.root.context.getColor(android.R.color.black))
        }

        // Cuando tocan la tarjeta, ejecutamos la acción
        binding.root.setOnClickListener { //binding.root - indica que estamos accediendo a TODO el componente padre(CardView)
            onThemeSelected(theme)
        }
    }
}
