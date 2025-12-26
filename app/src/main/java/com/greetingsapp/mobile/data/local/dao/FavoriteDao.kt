package com.greetingsapp.mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.greetingsapp.mobile.data.local.entities.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao { //en la interface se definen las reglas de para interacturar con la bd(room)

    // 1. Obtener todos los favoritos (Reactivo)
    @Query("SELECT * FROM favorites_table ORDER BY id DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>> //al usar flows cada que se actualice la lista de favoritos, en la pantalla del usuario se actualizaran automaticamente

    // 2. Insertar un favorito
    @Insert(onConflict = OnConflictStrategy.REPLACE) //Si se intenta guardar la misma imagen dos veces (mismo ID), esta regla dice "Borra la vieja y pon la nueva"
    suspend fun insertFavorite(favorite: FavoriteEntity) //todo metodo/funcion que realice una operacion de escritura, eliminacion o actualizacion
    // debe lanzarse en una CORRUTINA PARA QUE NO SE CONGELE LA PANTALLA

    @Delete// 3. Borrar un favorito
    suspend fun deleteFavorite(favorite: FavoriteEntity)

    //4. Borrar un favorito atraves usando la url
    @Query("DELETE FROM favorites_table WHERE image_url = :url")
    suspend fun deleteByUrl(url: String)

    // 4. Verificar si ya existe (para pintar el corazón)
    @Query("SELECT EXISTS(SELECT 1 FROM favorites_table WHERE image_url = :url)") //en caso de que se encuentre una coincidencia se retorna 1, EXISTS traduce si el retorno es 1 devuelve true, si es 0 devuelve false
    suspend fun isFavorite(url: String): Boolean
}