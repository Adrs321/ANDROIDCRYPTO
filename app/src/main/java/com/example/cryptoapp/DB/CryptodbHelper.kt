package com.example.cryptoapp.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.cryptoapp.api.CryptoCoin

class CryptoDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "cryptoapp.db"
        private const val DB_VERSION = 2 // Version incrementada por cambio de esquema

        const val TABLE_CRYPTO = "crypto_local"
        const val COL_ID = "id"
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
    }

    override fun onCreate(db: SQLiteDatabase) {
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
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CRYPTO")
        onCreate(db)
    }

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
                    imageUrl = c.getString(imageUrlIndex), // getString puede devolver null
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
