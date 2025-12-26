package com.greetingsapp.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greetingsapp.mobile.data.local.dao.FavoriteDao
import com.greetingsapp.mobile.data.local.entities.FavoriteEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImageDetailViewModel(private val favoriteDao: FavoriteDao) : ViewModel() {

    // 1. EL ESTADO (La verdad única)
    // _isFavorite es privado y mutable (nosotros lo cambiamos)
    // isFavorite es público e inmutable (la Activity solo lo "observa")

    private val _isFavorite = MutableStateFlow(false)

    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // 2. Verificar al iniciar
    fun checkFavoriteStatus(imageUrl: String) {
        viewModelScope.launch {
            // Preguntamos a la base de datos (rápido y en segundo plano)
            val exists = favoriteDao.isFavorite(imageUrl)
            _isFavorite.value = exists
        }
    }

    // 3. La Acción del Botón (Toggle)
    fun toggleFavorite(imageUrl: String, categoryTitle: String) {
        viewModelScope.launch {

            val currentStatus = _isFavorite.value //valor/estado actual

            // Creamos una instancia temporal de la Entidad (la fila de la tabla).
            val entity = FavoriteEntity(imageUrl = imageUrl, categoryTitle = categoryTitle)

            // Lógica de negocio simple:
            if (currentStatus) {
                // Si ya era True (favorito), significa que el usuario quiere borrarlo.
                // Llamamos al DAO para ejecutar el DELETE SQL.
                favoriteDao.deleteByUrl(imageUrl)
            } else {
                // Si era False, el usuario quiere guardarlo.
                // Llamamos al DAO para ejecutar el INSERT SQL.
                favoriteDao.insertFavorite(entity)
            }
            // ACTUALIZACIÓN OPTIMISTA DE UI:
            // Invertimos el valor manualmente (!currentStatus).
            // Ejemplo: Si era true, ahora ponemos false.
            // Esto dispara la señal a la Activity para que cambie el color del corazón.
            _isFavorite.value = !currentStatus
        }
    }
}

// Implementamos ViewModelProvider.Factory, que es una interfaz estándar de Android
// para enseñar al sistema cómo "fabricar" ViewModels personalizados.
class ImageDetailViewModelFactory(private val dao: FavoriteDao) : ViewModelProvider.Factory {

    // Este metodo es llamado internamente por Android cuando usas ViewModelProvider(this)[...]
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        // Verifica si la clase que piden es compatible con ImageDetailViewModel
        if (modelClass.isAssignableFrom(ImageDetailViewModel::class.java)) {

            // AQUI OCURRE LA INYECCIÓN DE DEPENDENCIA:
            // Creamos manualmente la instancia pasando el 'dao' que recibimos.
            // El "as T" es un casteo genérico necesario por la firma del metodo.
            @Suppress("UNCHECKED_CAST")
            return ImageDetailViewModel(dao) as T
        }
        // Si piden una clase rara, lanzamos error.
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}