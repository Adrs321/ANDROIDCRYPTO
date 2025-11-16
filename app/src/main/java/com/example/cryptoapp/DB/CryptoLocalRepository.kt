package com.example.cryptoapp.db

import android.content.Context
import com.example.cryptoapp.api.CryptoCoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Este objeto maneja las operaciones de la DB (leer, escribir)
 * fuera del hilo principal, usando Coroutines.
 * Idéntico a tu AlumnosLocalRepository.
 */
object CryptoLocalRepository {

    /** Inserta una LISTA de monedas (borrando las anteriores) */
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
}
