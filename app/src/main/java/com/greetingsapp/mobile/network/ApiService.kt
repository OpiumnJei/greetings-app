package com.greetingsapp.mobile.network

import com.greetingsapp.mobile.model.CategoryModel
import com.greetingsapp.mobile.model.ImageModel
import com.greetingsapp.mobile.model.PageResponse
import com.greetingsapp.mobile.model.ThemeModel
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    /**
     * Por que se retorna Call y no Response?
     *
     *   Call: Es el "control remoto" de la petición (permite iniciarla,
     *   cancelarla o reintentarla).
     *
     *   Response: Es lo que llega dentro del Callback cuando la petición termina.
     * */

    // 1. Obtener todas las categorías
    @GET("/api/categories")
    fun getCategories(): Call<List<CategoryModel>> //

    // 2. Obtener temáticas de una categoría
    @GET("/api/categories/{categoryId}/themes")
    fun getThemesByCategory(@Path("categoryId") categoryId: Long): Call<List<ThemeModel>>

    // 3. Obtener imágenes de una temática (Paginada)
    @GET("/api/themes/{themeId}/images")
    fun getImagesByTheme(
        @Path("themeId") themeId: Long,
        @Query("page") page: Int = 0, // Paginación por defecto página 0
        @Query("size") size: Int = 10 // 10 fotos por página
    ): Call<PageResponse<ImageModel>>
    // Fíjate que devolvemos PageResponse, no List directa, porque tu API devuelve una página
}