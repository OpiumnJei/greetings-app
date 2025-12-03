package com.greetingsapp.mobile.adapter

import androidx.recyclerview.widget.RecyclerView
import com.greetingsapp.mobile.databinding.ItemThemeBinding
import com.greetingsapp.mobile.model.ThemeModel

class ThemeViewHolder(private val binding: ItemThemeBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(theme: ThemeModel, onThemeSelected: (ThemeModel) -> Unit) { //onThemeSelected recibe un objeto del tipo The
        binding.textThemeName.text = theme.themeName

        // Cuando tocan la tarjeta, ejecutamos la acción
        binding.root.setOnClickListener { //binding.root - indica que estamos accediendo a TODO el componente padre(CardView)
            onThemeSelected(theme)
        }
    }
}
