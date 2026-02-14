package com.greetingsapp.mobile.data.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Objeto singleton para centralizar la configuración y creación del cliente Retrofit.
object RetrofitClient {

    // ✅ URL de producción (HTTPS)
    private const val BASE_URL = "https://images-api-1ob7.onrender.com/"

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Añade un convertidor para deserializar las respuestas JSON a objetos Kotlin/Java.
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}