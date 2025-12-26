package com.greetingsapp.mobile.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites_table") //indicamos el nombre de la tabla a Room
data class FavoriteEntity( //usamos una data class debido a que guardaremos datos

    @PrimaryKey(autoGenerate = true) // ids autogenereados
    val id: Int = 0, // val id: Int = 0: La inicializamos en 0 porque, al ser autogenerada,
    // Room ignorará este cero y pondrá el número correcto al guardar.

    @ColumnInfo(name = "image_url") //nombre del campo en la bd, referenciado por imageUrl
    val imageUrl: String,

    @ColumnInfo(name = "category_title")
    val categoryTitle: String
)