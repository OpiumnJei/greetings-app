package com.greetingsapp.mobile.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.greetingsapp.mobile.databinding.ItemThemeBinding
import com.greetingsapp.mobile.data.model.ThemeModel

// Recibimos una función "onThemeSelected" para avisar a la Activity cuando toquen un tema
class ThemesAdapter(private val onThemeSelected: (ThemeModel) -> Unit)//// funcion lambda que recibe un objeto ThemeModel(representacion de una tematica) y no retorna nada(unit)
    : ListAdapter<ThemeModel, ThemeViewHolder>(DiffCallback) {

    // ⭐ Variable para guardar cuál está seleccionado
    private var selectedThemeId: Long? = null

    // ⭐ Metodo para marcar uno desde fuera (HomeFragment)
    fun selectThemeById(id: Long) {
        selectedThemeId = id
        notifyDataSetChanged() // Refresca la lista para pintar el cambio
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemThemeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = getItem(position)
        // si id el primer elemento de la lista coincide con el id del elemento seleccionado
        val isSelected = (theme.themeId == selectedThemeId)
        holder.bind(theme,isSelected, onThemeSelected) //se le pasa la funcion lambda como parametro
    }


    companion object DiffCallback : DiffUtil.ItemCallback<ThemeModel>() {
        override fun areItemsTheSame(oldItem: ThemeModel, newItem: ThemeModel) =
            oldItem.themeId == newItem.themeId

        override fun areContentsTheSame(oldItem: ThemeModel, newItem: ThemeModel) =
            oldItem == newItem
    }
}