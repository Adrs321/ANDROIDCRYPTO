package com.example.cryptoapp.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cryptoapp.api.CryptoCoin
import java.security.MessageDigest

class CryptoDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "cryptoapp.db"
        private const val DB_VERSION = 4 // Versión incrementada para añadir favoritos

        // Tablas
        const val TABLE_CRYPTO = "crypto_local"
        const val TABLE_USERS = "users"
        const val TABLE_FAVORITES = "user_favorites" // NUEVO

        // Columnas comunes
        const val COL_ID = "id"

        // Columnas de Cripto
        const val COL_NAME = "name"
        const val COL_SYMBOL = "symbol"
        const val COL_IMAGE_URL = "image_url"
        const val COL_PRICE = "price_usd"
        const val COL_MARKET_CAP = "market_cap"
        const val COL_MARKET_CAP_RANK = "market_cap_rank"
        const val COL_TOTAL_VOLUME = "total_volume"
        const val COL_CHANGE = "change_24h"
        const val COL_TOTAL_SUPPLY = "total_supply"
        const val COL_MAX_SUPPLY = "max_supply"

        // Columnas de Usuario
        const val COL_USER_ID = "user_id"
        const val COL_USER_NAME = "user_name"
        const val COL_USER_EMAIL = "user_email"
        const val COL_USER_PASSWORD = "user_password"

        // Columnas de Favoritos (NUEVO)
        const val COL_FAVORITE_ID = "favorite_id"
        const val COL_FAVORITE_USER_ID = "user_id"
        const val COL_FAVORITE_CRYPTO_ID = "crypto_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Crear tabla de criptos
        db.execSQL(
            """
            CREATE TABLE $TABLE_CRYPTO (
                $COL_ID TEXT PRIMARY KEY,
                $COL_NAME TEXT NOT NULL,
                $COL_SYMBOL TEXT NOT NULL,
                $COL_IMAGE_URL TEXT,
                $COL_PRICE REAL,
                $COL_MARKET_CAP INTEGER,
                $COL_MARKET_CAP_RANK INTEGER,
                $COL_TOTAL_VOLUME REAL,
                $COL_CHANGE REAL,
                $COL_TOTAL_SUPPLY REAL,
                $COL_MAX_SUPPLY REAL
            );
            """.trimIndent()
        )

        // Crear tabla de usuarios
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_NAME TEXT NOT NULL,
                $COL_USER_EMAIL TEXT NOT NULL UNIQUE,
                $COL_USER_PASSWORD TEXT NOT NULL
            );
            """.trimIndent()
        )

        // Crear tabla de favoritos (NUEVO)
        db.execSQL(
            """
            CREATE TABLE $TABLE_FAVORITES (
                $COL_FAVORITE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FAVORITE_USER_ID INTEGER NOT NULL,
                $COL_FAVORITE_CRYPTO_ID TEXT NOT NULL,
                FOREIGN KEY($COL_FAVORITE_USER_ID) REFERENCES $TABLE_USERS($COL_USER_ID),
                UNIQUE($COL_FAVORITE_USER_ID, $COL_FAVORITE_CRYPTO_ID)
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CRYPTO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES") // NUEVO
        onCreate(db)
    }

    // --- Funciones para Usuarios ---

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    fun addUser(name: String, email: String, password: String): Long {
        val cv = ContentValues().apply {
            put(COL_USER_NAME, name)
            put(COL_USER_EMAIL, email)
            put(COL_USER_PASSWORD, hashPassword(password))
        }
        return writableDatabase.insert(TABLE_USERS, null, cv)
    }

    fun checkUser(email: String, password: String): Boolean {
        val hashedPassword = hashPassword(password)
        val selection = "$COL_USER_EMAIL = ? AND $COL_USER_PASSWORD = ?"
        val selectionArgs = arrayOf(email, hashedPassword)
        readableDatabase.query(
            TABLE_USERS, null, selection, selectionArgs, null, null, null
        ).use { cursor ->
            return cursor.count > 0
        }
    }

    fun getUserId(email: String): Int {
        readableDatabase.query(TABLE_USERS, arrayOf(COL_USER_ID), "$COL_USER_EMAIL = ?", arrayOf(email), null, null, null)
            .use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID))
                }
            }
        return -1 // Devuelve -1 si no se encuentra el usuario
    }
    
    // --- Funciones para Favoritos (NUEVO) ---

    fun addFavorite(userId: Int, cryptoId: String) {
        val cv = ContentValues().apply {
            put(COL_FAVORITE_USER_ID, userId)
            put(COL_FAVORITE_CRYPTO_ID, cryptoId)
        }
        writableDatabase.insertWithOnConflict(TABLE_FAVORITES, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun removeFavorite(userId: Int, cryptoId: String) {
        val whereClause = "$COL_FAVORITE_USER_ID = ? AND $COL_FAVORITE_CRYPTO_ID = ?"
        writableDatabase.delete(TABLE_FAVORITES, whereClause, arrayOf(userId.toString(), cryptoId))
    }

    fun isFavorite(userId: Int, cryptoId: String): Boolean {
        val selection = "$COL_FAVORITE_USER_ID = ? AND $COL_FAVORITE_CRYPTO_ID = ?"
        val selectionArgs = arrayOf(userId.toString(), cryptoId)
        readableDatabase.query(TABLE_FAVORITES, null, selection, selectionArgs, null, null, null)
            .use { cursor ->
                return cursor.count > 0
            }
    }

    fun getFavoriteCoins(userId: Int): List<CryptoCoin> {
        val cryptoList = mutableListOf<CryptoCoin>()
        // Consulta SQL que une la tabla de favoritos con la de criptomonedas
        val sql = """
            SELECT T2.* FROM $TABLE_FAVORITES T1
            JOIN $TABLE_CRYPTO T2 ON T1.$COL_FAVORITE_CRYPTO_ID = T2.$COL_ID
            WHERE T1.$COL_FAVORITE_USER_ID = ?
            ORDER BY T2.$COL_MARKET_CAP_RANK ASC
        """
        readableDatabase.rawQuery(sql, arrayOf(userId.toString())).use { c ->
            // El resto de la lógica es idéntica a getAll()
            val idIndex = c.getColumnIndexOrThrow(COL_ID)
            val symbolIndex = c.getColumnIndexOrThrow(COL_SYMBOL)
            val nameIndex = c.getColumnIndexOrThrow(COL_NAME)
            val imageUrlIndex = c.getColumnIndexOrThrow(COL_IMAGE_URL)
            val priceUsdIndex = c.getColumnIndexOrThrow(COL_PRICE)
            val marketCapIndex = c.getColumnIndexOrThrow(COL_MARKET_CAP)
            val marketCapRankIndex = c.getColumnIndexOrThrow(COL_MARKET_CAP_RANK)
            val totalVolumeIndex = c.getColumnIndexOrThrow(COL_TOTAL_VOLUME)
            val changePercent24HrIndex = c.getColumnIndexOrThrow(COL_CHANGE)
            val totalSupplyIndex = c.getColumnIndexOrThrow(COL_TOTAL_SUPPLY)
            val maxSupplyIndex = c.getColumnIndexOrThrow(COL_MAX_SUPPLY)

            while (c.moveToNext()) {
                cryptoList += CryptoCoin(
                    id = c.getString(idIndex),
                    symbol = c.getString(symbolIndex),
                    name = c.getString(nameIndex),
                    imageUrl = if (c.isNull(imageUrlIndex)) null else c.getString(imageUrlIndex),
                    priceUsd = if (c.isNull(priceUsdIndex)) null else c.getDouble(priceUsdIndex),
                    marketCap = if (c.isNull(marketCapIndex)) null else c.getLong(marketCapIndex),
                    marketCapRank = if (c.isNull(marketCapRankIndex)) null else c.getInt(marketCapRankIndex),
                    totalVolume = if (c.isNull(totalVolumeIndex)) null else c.getDouble(totalVolumeIndex),
                    changePercent24Hr = if (c.isNull(changePercent24HrIndex)) null else c.getDouble(changePercent24HrIndex),
                    totalSupply = if (c.isNull(totalSupplyIndex)) null else c.getDouble(totalSupplyIndex),
                    maxSupply = if (c.isNull(maxSupplyIndex)) null else c.getDouble(maxSupplyIndex)
                )
            }
        }
        return cryptoList
    }

    // --- Funciones para Criptomonedas (sin cambios) ---

    fun insert(coin: CryptoCoin): Long {
        val cv = ContentValues().apply {
            put(COL_ID, coin.id)
            put(COL_NAME, coin.name)
            put(COL_SYMBOL, coin.symbol)
            put(COL_IMAGE_URL, coin.imageUrl)
            put(COL_PRICE, coin.priceUsd)
            put(COL_MARKET_CAP, coin.marketCap)
            put(COL_MARKET_CAP_RANK, coin.marketCapRank)
            put(COL_TOTAL_VOLUME, coin.totalVolume)
            put(COL_CHANGE, coin.changePercent24Hr)
            put(COL_TOTAL_SUPPLY, coin.totalSupply)
            put(COL_MAX_SUPPLY, coin.maxSupply)
        }
        return writableDatabase.insertWithOnConflict(TABLE_CRYPTO, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAll(): List<CryptoCoin> {
        val cryptoList = mutableListOf<CryptoCoin>()
        val sql = "SELECT * FROM $TABLE_CRYPTO ORDER BY $COL_MARKET_CAP_RANK ASC"

        readableDatabase.rawQuery(sql, null).use { c ->
            val idIndex = c.getColumnIndexOrThrow(COL_ID)
            val symbolIndex = c.getColumnIndexOrThrow(COL_SYMBOL)
            val nameIndex = c.getColumnIndexOrThrow(COL_NAME)
            val imageUrlIndex = c.getColumnIndexOrThrow(COL_IMAGE_URL)
            val priceUsdIndex = c.getColumnIndexOrThrow(COL_PRICE)
            val marketCapIndex = c.getColumnIndexOrThrow(COL_MARKET_CAP)
            val marketCapRankIndex = c.getColumnIndexOrThrow(COL_MARKET_CAP_RANK)
            val totalVolumeIndex = c.getColumnIndexOrThrow(COL_TOTAL_VOLUME)
            val changePercent24HrIndex = c.getColumnIndexOrThrow(COL_CHANGE)
            val totalSupplyIndex = c.getColumnIndexOrThrow(COL_TOTAL_SUPPLY)
            val maxSupplyIndex = c.getColumnIndexOrThrow(COL_MAX_SUPPLY)

            while (c.moveToNext()) {
                cryptoList += CryptoCoin(
                    id = c.getString(idIndex),
                    symbol = c.getString(symbolIndex),
                    name = c.getString(nameIndex),
                    imageUrl = if (c.isNull(imageUrlIndex)) null else c.getString(imageUrlIndex),
                    priceUsd = if (c.isNull(priceUsdIndex)) null else c.getDouble(priceUsdIndex),
                    marketCap = if (c.isNull(marketCapIndex)) null else c.getLong(marketCapIndex),
                    marketCapRank = if (c.isNull(marketCapRankIndex)) null else c.getInt(marketCapRankIndex),
                    totalVolume = if (c.isNull(totalVolumeIndex)) null else c.getDouble(totalVolumeIndex),
                    changePercent24Hr = if (c.isNull(changePercent24HrIndex)) null else c.getDouble(changePercent24HrIndex),
                    totalSupply = if (c.isNull(totalSupplyIndex)) null else c.getDouble(totalSupplyIndex),
                    maxSupply = if (c.isNull(maxSupplyIndex)) null else c.getDouble(maxSupplyIndex)
                )
            }
        }
        return cryptoList
    }

    fun clear() {
        writableDatabase.delete(TABLE_CRYPTO, null, null)
    }
}
