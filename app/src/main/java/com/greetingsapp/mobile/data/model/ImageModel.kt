package com.greetingsapp.mobile.data.model

import com.google.gson.annotations.SerializedName

data class ImageModel(
    @SerializedName("imageId")
    val imageId: Long,

    @SerializedName("imageName")
    val imageName: String,

    @SerializedName("imageDescription")
    val imageDescription: String,

    @SerializedName("imageUrl")
    val imageUrl: String
)