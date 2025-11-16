package com.example.cryptoapp.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * Objeto Singleton para configurar y crear el cliente de Retrofit.
 * Es idéntico a tu ApiDuocClient, pero apunta a la URL de CoinGecko.
 */
object CryptoApiClient {


    private const val BASE_URL = "https://api.coingecko.com/"


    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()


    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    private val http = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()


    val service: CryptoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(http)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CryptoApiService::class.java)
    }
}
