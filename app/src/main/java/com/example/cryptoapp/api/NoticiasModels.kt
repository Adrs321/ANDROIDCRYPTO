package com.example.cryptoapp.api

import com.squareup.moshi.Json

// La API de CryptoCompare devuelve un objeto raiz, y dentro una lista "Data"
data class NewsResponse(
    @Json(name = "Data") val data: List<NewsArticle>
)

data class NewsArticle(
    val id: String,
    val title: String,
    val body: String, // El resumen de la noticia
    val url: String,  // Link a la noticia completa
    @Json(name = "imageurl") val imageUrl: String?,
    @Json(name = "source_info") val sourceInfo: SourceInfo?
)

data class SourceInfo(
    val name: String
)