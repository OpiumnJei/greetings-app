package com.greetingsapp.mobile.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Objeto singleton para centralizar la configuración y creación del cliente Retrofit.
object RetrofitClient {

    // TRUCO: "10.0.2.2" es la dirección mágica que usa el Emulador de Android
    // para referirse a tu "localhost" de la computadora.
    // Si pones "localhost", el emulador se buscaría a sí mismo y fallaría.
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Añade un convertidor para deserializar las respuestas JSON a objetos Kotlin/Java.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}