package com.example.cryptoapp.api

import com.squareup.moshi.Json

data class Comment(
    @Json(name = "id") val id: String? = null, // ID generado por MockAPI (String)
    @Json(name = "newsId") val newsId: String,
    @Json(name = "userId") val userId: Int,
    @Json(name = "userName") val userName: String,
    @Json(name = "text") val text: String,
    @Json(name = "date") val date: Long
)