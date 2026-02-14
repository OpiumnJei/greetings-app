package com.greetingsapp.mobile.data.model

import com.google.gson.annotations.SerializedName

// clase que representa la entidad category del backend
data class CategoryModel(
    @SerializedName("categoryId")
    val categoryId: Long,

    @SerializedName("categoryName")
    val categoryName: String
)