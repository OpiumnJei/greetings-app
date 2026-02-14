package com.greetingsapp.mobile.data.model

import com.google.gson.annotations.SerializedName

data class ThemeModel(
    @SerializedName("themeId")
    val themeId: Long,

    @SerializedName("themeName")
    val themeName: String
)