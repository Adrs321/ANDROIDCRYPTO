package com.example.cryptoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cryptoapp.api.CryptoCoin
import com.example.cryptoapp.db.CryptoDbHelper

class FavoriteCoinsActivity : AppCompatActivity() {

    private lateinit var dbHelper: CryptoDbHelper
    private lateinit var favoriteCryptoList: ListView
    private lateinit var tvNoFavorites: TextView
    private var userId: Int = -1
    private var favoriteCoins: List<CryptoCoin> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        dbHelper = CryptoDbHelper(this)
        favoriteCryptoList = findViewById(R.id.lv_favorite_crypto_list)
        tvNoFavorites = findViewById(R.id.tv_no_favorites)

        val session = getSharedPreferences("session", MODE_PRIVATE)
        userId = session.getInt("userId", -1)

        favoriteCryptoList.setOnItemClickListener { _, _, position, _ ->
            val selectedCoin = favoriteCoins[position]
            val intent = Intent(this, CryptoDetailActivity::class.java).apply {
                putExtra("CRYPTO_COIN", selectedCoin)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1) {
            loadFavoriteCoins()
        }
    }

    private fun loadFavoriteCoins() {
        favoriteCoins = dbHelper.getFavoriteCoins(userId)

        if (favoriteCoins.isEmpty()) {
            tvNoFavorites.visibility = View.VISIBLE
            favoriteCryptoList.visibility = View.GONE
        } else {
            tvNoFavorites.visibility = View.GONE
            favoriteCryptoList.visibility = View.VISIBLE

            val adapter = ArrayAdapter(
                this,
                R.layout.list_item_crypto,
                R.id.text1,
                favoriteCoins.map { formatCoinForList(it) }
            )
            favoriteCryptoList.adapter = adapter
        }
    }

    private fun formatCoinForList(coin: CryptoCoin): String {
        val price = coin.priceUsd?.let { String.format("%.2f", it) } ?: "N/A"
        val change = coin.changePercent24Hr?.let { String.format("%.2f", it) } ?: "N/A"
        return "${coin.name} (${coin.symbol.uppercase()})\nPrecio: $${price} USD | Cambio 24h: ${change}%"
    }
}
