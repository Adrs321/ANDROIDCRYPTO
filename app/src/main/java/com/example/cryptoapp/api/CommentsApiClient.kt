package com.example.cryptoapp.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor


// 1. La Interfaz con los métodos CRUD
interface CommentsApiService {

    // LEER (Read) - Obtener todos los comentarios
    // (Filtraríamos en la app, o MockAPI permite filtrar por newsId así: ?newsId=...)
    @GET("comments")
    suspend fun getComments(@Query("newsId") newsId: String): List<Comment>

    // CREAR (Create)
    @POST("comments")
    suspend fun createComment(@Body comment: Comment): Comment

    // MODIFICAR (Update)
    @PUT("comments/{id}")
    suspend fun updateComment(@Path("id") id: String, @Body comment: Comment): Comment

    // ELIMINAR (Delete)
    @DELETE("comments/{id}")
    suspend fun deleteComment(@Path("id") id: String): Comment
}

// 2. El Cliente Retrofit (Singleton)
object CommentsApiClient {
    // URL verificada (¡No le quites la barra al final!)
    private const val BASE_URL = "https://69409048993d68afba6c7027.mockapi.io/"

    // 1. Configurar el "Espía" (Log) para ver errores en Logcat
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    // 2. Configurar Moshi para Kotlin (¡ESTO ES LO QUE FALTABA!)
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi)) // Usamos el moshi configurado
        .build()

    val service: CommentsApiService = retrofit.create(CommentsApiService::class.java)
}