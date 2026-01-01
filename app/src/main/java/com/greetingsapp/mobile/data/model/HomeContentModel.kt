package com.greetingsapp.mobile.data.model

import com.google.gson.annotations.SerializedName

// clase que representa la entidad SpecialDay del backend
data class HomeContentModel(
    @SerializedName("type")  // ← ESTO ES CRUCIAL
    val contentType: String,  // Mapea "type" del JSON a contentType en Kotlin

    @SerializedName("title")
    val title: String,

    @SerializedName("images")
    val images: List<ImageModel>)