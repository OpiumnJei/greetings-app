package com.greetingsapp.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greetingsapp.mobile.data.local.dao.FavoriteDao
import com.greetingsapp.mobile.data.local.entities.FavoriteEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FavoritesViewModel(private val dao: FavoriteDao) : ViewModel() {

    // --- LA TUBERÍA DE DATOS ---
    // Convertimos el Flow de la BD en un StateFlow que la vista pueda "observar".
    // stateIn: Mantiene el flujo activo mientras la UI lo necesite.
    val favoritesList: StateFlow<List<FavoriteEntity>> = dao.getAllFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Se pausa si la app va a segundo plano por 5seg
            initialValue = emptyList() // Empieza vacía mientras carga
        )
}

// --- LA FÁBRICA (Igual que la anterior) ---
class FavoritesViewModelFactory(private val dao: FavoriteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FavoritesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FavoritesViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}