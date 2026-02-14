package com.greetingsapp.mobile.data.model

import com.google.gson.annotations.SerializedName

// Esta clase "envuelve" la lista que manda Spring Boot en su objeto Page
data class PageResponse<T>(
    @SerializedName("content")
    val content: List<T>,     // Aquí viene tu lista real

    @SerializedName("totalElements")
    val totalElements: Long,

    @SerializedName("totalPages")
    val totalPages: Int
    // Puedes agregar más campos si los necesitas (size, number, etc.)
)