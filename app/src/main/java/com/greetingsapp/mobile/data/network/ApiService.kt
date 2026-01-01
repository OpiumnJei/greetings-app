package com.greetingsapp.mobile.data.network

import com.greetingsapp.mobile.data.model.CategoryModel
import com.greetingsapp.mobile.data.model.HomeContentModel
import com.greetingsapp.mobile.data.model.ImageModel
import com.greetingsapp.mobile.data.model.PageResponse
import com.greetingsapp.mobile.data.model.ThemeModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {


    // 1. Obtener todas las categorías
    @GET("/api/categories")
    suspend fun getCategories(): Response<List<CategoryModel>> // Response nos ayuda a manejar las respuestas del servidor de manera mas elegante

    // 2. Obtener temáticas de una categoría
    @GET("/api/categories/{categoryId}/themes")
    suspend fun getThemesByCategory(@Path("categoryId") categoryId: Long): Response<List<ThemeModel>>

    // 3. Obtener imágenes de una temática (Paginada)
    @GET("/api/themes/{themeId}/images")
    suspend fun getImagesByTheme(
        @Path("themeId") themeId: Long,
        @Query("page") page: Int = 0, // Paginación por defecto página 0
        @Query("size") size: Int = 10 // 10 fotos por página
    ): Response<PageResponse<ImageModel>>
    // Fíjate que devolvemos PageResponse, no List directa, porque tu API devuelve una página

    // 4. Traer TODAS las imágenes (para la pantalla de Inicio/Novedades)
    // Asumimos que tu backend tiene un endpoint /api/images que soporta paginación
    @GET("/api/images")
    suspend fun getAllImages(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20, // Traemos más para llenar la pantalla
        @Query("sort") sort: String = "created,desc" // Truco: Pedir las más nuevas primero
    ): Response<PageResponse<ImageModel>>

    // 5. Trae contenido dinamico en base a la fecha actual para cargarlo en el menu
    @GET("/api/home")
    suspend fun getHomeContent(): Response<HomeContentModel>
}