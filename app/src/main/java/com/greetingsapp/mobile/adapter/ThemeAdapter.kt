package com.greetingsapp.mobile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.greetingsapp.mobile.databinding.ItemThemeBinding
import com.greetingsapp.mobile.model.ThemeModel

// Recibimos una función "onThemeSelected" para avisar a la Activity cuando toquen un tema
class ThemesAdapter(private val onThemeSelected: (ThemeModel) -> Unit)//// funcion lambda que recibe un objeto ThemeModel(representacion de una tematica) y no retorna nada(unit)
    : ListAdapter<ThemeModel, ThemeViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = getItem(position)
        holder.bind(theme, onThemeSelected) //se le pasa la funcion lambda como parametro
    }


    companion object DiffCallback : DiffUtil.ItemCallback<ThemeModel>() {
        override fun areItemsTheSame(oldItem: ThemeModel, newItem: ThemeModel) =
            oldItem.themeId == newItem.themeId

        override fun areContentsTheSame(oldItem: ThemeModel, newItem: ThemeModel) =
            oldItem == newItem
    }
}