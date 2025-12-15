package com.example.cryptoapp.db

import android.content.Context
import com.example.cryptoapp.api.CryptoCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object CryptoLocalRepository {


    suspend fun insertMany(context: Context, coins: List<CryptoCoin>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                CryptoDbHelper(context).use { helper ->
                    // 1. Borra datos viejos
                    helper.clear()
                    // 2. Inserta datos nuevos
                    coins.forEach { coin ->
                        helper.insert(coin)
                    }
                }
            }
        }

    /** Obtiene todas las monedas de la DB local */
    suspend fun getAll(context: Context): Result<List<CryptoCoin>> =
        withContext(Dispatchers.IO) {
            runCatching {
                CryptoDbHelper(context).use { it.getAll() }
            }
        }

    /** Borra todos los datos (opcional, 'insertMany' ya lo hace) */
    suspend fun clear(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                CryptoDbHelper(context).use { it.clear() }
            }
        }

    suspend fun register(context: Context, name: String, email: String, pass: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Llama a addUser del Helper
                CryptoDbHelper(context).use { it.addUser(name, email, pass) != -1L }
            }
        }
}
