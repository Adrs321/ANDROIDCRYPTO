package com.example.cryptoapp.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cryptoapp.api.CryptoCoin
import com.example.cryptoapp.api.NewsArticle
import com.example.cryptoapp.api.SourceInfo
import java.security.MessageDigest

// Clase de datos para manejar las alertas (definida fuera para acceso global)
data class AlertData(val id: Int, val symbol: String, val targetPrice: Double, val isAbove: Boolean)

class CryptoDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "cryptoapp.db"
        private const val DB_VERSION = 7 // Versión actualizada para Noticias Favoritas

        // Tablas
        const val TABLE_CRYPTO = "crypto_local"
        const val TABLE_USERS = "users"
        const val TABLE_FAVORITES = "user_favorites"
        const val TABLE_ALERTS = "alerts"
        const val TABLE_FAV_NEWS = "favorite_news" // NUEVO

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

        // Columnas de Favoritos
        const val COL_FAVORITE_ID = "favorite_id"
        const val COL_FAVORITE_USER_ID = "user_id"
        const val COL_FAVORITE_CRYPTO_ID = "crypto_id"

        // Columnas de Alertas
        const val COL_ALERT_ID = "alert_id"
        const val COL_ALERT_USER_ID = "user_id"
        const val COL_ALERT_SYMBOL = "symbol"
        const val COL_ALERT_TARGET_PRICE = "target"
        const val COL_ALERT_IS_ABOVE = "is_above"

        // Columnas de Noticias Favoritas (NUEVO)
        const val COL_NEWS_ID = "news_id"
        const val COL_NEWS_USER_ID = "user_id"
        const val COL_NEWS_ARTICLE_ID = "article_id"
        const val COL_NEWS_TITLE = "title"
        const val COL_NEWS_URL = "url"
        const val COL_NEWS_IMAGE = "image_url"
        const val COL_NEWS_SOURCE = "source_name"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Tabla Criptos
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

        // 2. Tabla Usuarios
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

        // 3. Tabla Favoritos (Monedas)
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

        // 4. Tabla Alertas
        db.execSQL(
            """
            CREATE TABLE $TABLE_ALERTS (
                $COL_ALERT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_ALERT_USER_ID INTEGER NOT NULL,
                $COL_ALERT_SYMBOL TEXT NOT NULL,
                $COL_ALERT_TARGET_PRICE REAL NOT NULL,
                $COL_ALERT_IS_ABOVE INTEGER NOT NULL
            );
            """.trimIndent()
        )

        // 5. Tabla Noticias Favoritas (NUEVO)
        db.execSQL(
            """
            CREATE TABLE $TABLE_FAV_NEWS (
                $COL_NEWS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NEWS_USER_ID INTEGER NOT NULL,
                $COL_NEWS_ARTICLE_ID TEXT NOT NULL,
                $COL_NEWS_TITLE TEXT,
                $COL_NEWS_URL TEXT,
                $COL_NEWS_IMAGE TEXT,
                $COL_NEWS_SOURCE TEXT,
                UNIQUE($COL_NEWS_USER_ID, $COL_NEWS_ARTICLE_ID)
            );
            """.trimIndent()
        )

        // Insertar Admin por defecto
        val adminPass = hashPassword("admin")
        val cv = ContentValues().apply {
            put(COL_USER_NAME, "Administrador")
            put(COL_USER_EMAIL, "admin@admin.com")
            put(COL_USER_PASSWORD, adminPass)
        }
        db.insert(TABLE_USERS, null, cv)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CRYPTO")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ALERTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FAV_NEWS") // NUEVO
        onCreate(db)
    }

    // --- Funciones de Usuarios ---

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
        return -1
    }

    fun getUserData(userId: Int): Pair<String, String>? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COL_USER_NAME, COL_USER_EMAIL),
            "$COL_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_NAME))
            val email = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_EMAIL))
            cursor.close()
            Pair(name, email)
        } else {
            cursor.close()
            null
        }
    }

    // --- Funciones de Criptos Favoritas ---

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
        val sql = """
            SELECT T2.* FROM $TABLE_FAVORITES T1
            JOIN $TABLE_CRYPTO T2 ON T1.$COL_FAVORITE_CRYPTO_ID = T2.$COL_ID
            WHERE T1.$COL_FAVORITE_USER_ID = ?
            ORDER BY T2.$COL_MARKET_CAP_RANK ASC
        """
        readableDatabase.rawQuery(sql, arrayOf(userId.toString())).use { c ->
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

    // --- Funciones de Alertas ---

    fun addAlert(userId: Int, symbol: String, targetPrice: Double, currentPrice: Double): Long {
        val isAbove = if (targetPrice > currentPrice) 1 else 0
        val cv = ContentValues().apply {
            put(COL_ALERT_USER_ID, userId)
            put(COL_ALERT_SYMBOL, symbol)
            put(COL_ALERT_TARGET_PRICE, targetPrice)
            put(COL_ALERT_IS_ABOVE, isAbove)
        }
        return writableDatabase.insert(TABLE_ALERTS, null, cv)
    }

    fun getAlerts(userId: Int): List<AlertData> {
        val alerts = mutableListOf<AlertData>()
        val sql = "SELECT * FROM $TABLE_ALERTS WHERE $COL_ALERT_USER_ID = ?"

        readableDatabase.rawQuery(sql, arrayOf(userId.toString())).use { c ->
            val idIndex = c.getColumnIndexOrThrow(COL_ALERT_ID)
            val symbolIndex = c.getColumnIndexOrThrow(COL_ALERT_SYMBOL)
            val targetIndex = c.getColumnIndexOrThrow(COL_ALERT_TARGET_PRICE)
            val isAboveIndex = c.getColumnIndexOrThrow(COL_ALERT_IS_ABOVE)

            while (c.moveToNext()) {
                alerts.add(AlertData(
                    id = c.getInt(idIndex),
                    symbol = c.getString(symbolIndex),
                    targetPrice = c.getDouble(targetIndex),
                    isAbove = c.getInt(isAboveIndex) == 1
                ))
            }
        }
        return alerts
    }

    fun deleteAlert(alertId: Int) {
        writableDatabase.delete(TABLE_ALERTS, "$COL_ALERT_ID = ?", arrayOf(alertId.toString()))
    }

    // --- Funciones para Noticias Favoritas (NUEVO) ---

    fun addFavoriteNews(userId: Int, article: NewsArticle) {
        val cv = ContentValues().apply {
            put(COL_NEWS_USER_ID, userId)
            put(COL_NEWS_ARTICLE_ID, article.id)
            put(COL_NEWS_TITLE, article.title)
            put(COL_NEWS_URL, article.url)
            put(COL_NEWS_IMAGE, article.imageUrl)
            put(COL_NEWS_SOURCE, article.sourceInfo?.name ?: "Desconocido")
        }
        writableDatabase.insertWithOnConflict(TABLE_FAV_NEWS, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun removeFavoriteNews(userId: Int, articleId: String) {
        writableDatabase.delete(TABLE_FAV_NEWS,
            "$COL_NEWS_USER_ID = ? AND $COL_NEWS_ARTICLE_ID = ?",
            arrayOf(userId.toString(), articleId))
    }

    fun isFavoriteNews(userId: Int, articleId: String): Boolean {
        readableDatabase.query(TABLE_FAV_NEWS, null,
            "$COL_NEWS_USER_ID = ? AND $COL_NEWS_ARTICLE_ID = ?",
            arrayOf(userId.toString(), articleId), null, null, null).use {
            return it.count > 0
        }
    }

    fun getFavoriteNews(userId: Int): List<NewsArticle> {
        val newsList = mutableListOf<NewsArticle>()
        val sql = "SELECT * FROM $TABLE_FAV_NEWS WHERE $COL_NEWS_USER_ID = ? ORDER BY $COL_NEWS_ID DESC"

        readableDatabase.rawQuery(sql, arrayOf(userId.toString())).use { c ->
            val idIndex = c.getColumnIndexOrThrow(COL_NEWS_ARTICLE_ID)
            val titleIndex = c.getColumnIndexOrThrow(COL_NEWS_TITLE)
            val urlIndex = c.getColumnIndexOrThrow(COL_NEWS_URL)
            val imgIndex = c.getColumnIndexOrThrow(COL_NEWS_IMAGE)
            val sourceIndex = c.getColumnIndexOrThrow(COL_NEWS_SOURCE)

            while (c.moveToNext()) {
                newsList.add(NewsArticle(
                    id = c.getString(idIndex),
                    title = c.getString(titleIndex),
                    body = "", // Cuerpo vacío, no se guarda en DB local
                    url = c.getString(urlIndex),
                    imageUrl = c.getString(imgIndex),
                    sourceInfo = SourceInfo(c.getString(sourceIndex))
                ))
            }
        }
        return newsList
    }

    // --- Funciones para Criptomonedas ---

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