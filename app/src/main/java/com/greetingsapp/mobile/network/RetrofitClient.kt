package com.greetingsapp.mobile.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Objeto singleton para centralizar la configuración y creación del cliente Retrofit.
object RetrofitClient {

    // cambiamos el localhost, por la IP de la PC, el objetivo es no tener que depender de abd reverse, y hacerlo
    // T0do de forma inalambrica
    //ip pc: 192.168.0.104
    private const val BASE_URL = "http://192.168.0.104:8080/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Añade un convertidor para deserializar las respuestas JSON a objetos Kotlin/Java.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}