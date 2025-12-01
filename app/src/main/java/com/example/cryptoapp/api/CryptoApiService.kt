package com.example.cryptoapp.api

import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoApiService {


    @GET("api/v3/coins/markets")
    suspend fun getCryptoAssets(

        @Query("vs_currency") currency: String = "usd",
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1
    ): List<CryptoCoin> //

    @GET("https://min-api.cryptocompare.com/data/v2/news/?lang=EN")
    suspend fun getCryptoNews(): NewsResponse
}
