package com.example.cryptoapp.api

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.parcelize.Parcelize

@Parcelize
data class CryptoCoin(
    val id: String,
    val symbol: String,
    val name: String,
    @Json(name = "image") val imageUrl: String?,
    @Json(name = "current_price") val priceUsd: Double?,
    @Json(name = "market_cap") val marketCap: Long?,
    @Json(name = "market_cap_rank") val marketCapRank: Int?,
    @Json(name = "total_volume") val totalVolume: Double?,
    @Json(name = "price_change_percentage_24h") val changePercent24Hr: Double?,
    @Json(name = "total_supply") val totalSupply: Double?,
    @Json(name = "max_supply") val maxSupply: Double?
) : Parcelable