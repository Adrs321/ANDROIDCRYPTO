package com.example.cryptoapp.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CryptoRepository {


    suspend fun fetchCryptoAssets(): Result<List<CryptoCoin>> = withContext(Dispatchers.IO) {
        try {

            val response = CryptoApiClient.service.getCryptoAssets()
            Result.success(response)
        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
