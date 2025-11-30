package com.greetingsapp.mobile.model

// Esta clase "envuelve" la lista que manda Spring Boot en su objeto Page
data class PageResponse<T>(
    val content: List<T>,     // Aquí viene tu lista real
    val totalElements: Long,
    val totalPages: Int
    // Puedes agregar más campos si los necesitas (size, number, etc.)
)