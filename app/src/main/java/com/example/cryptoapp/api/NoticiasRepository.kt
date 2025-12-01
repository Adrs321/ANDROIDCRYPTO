package com.example.cryptoapp.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object NoticiasRepository {

    suspend fun fetchNews(): Result<List<NewsArticle>> = withContext(Dispatchers.IO) {
        try {
            val response = CryptoApiClient.service.getCryptoNews()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}