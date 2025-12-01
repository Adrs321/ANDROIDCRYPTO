package com.example.cryptoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Inicio : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        val btnCoinList = findViewById<Button>(R.id.btn_coin_list)
        val btnFavoriteCoins = findViewById<Button>(R.id.btn_favorite_coins)
        val btnNews = findViewById<Button>(R.id.btn_news)
        val btnSettings = findViewById<Button>(R.id.btn_settings)
        val btnLogout = findViewById<Button>(R.id.btn_logout)

        // Navegar a la lista de monedas
        btnCoinList.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Navegar a la lista de monedas favoritas
        btnFavoriteCoins.setOnClickListener {
            startActivity(Intent(this, FavoriteCoinsActivity::class.java))
        }

        // Navegar a las noticias
        btnNews.setOnClickListener {
            startActivity(Intent(this, NewsActivity::class.java))
        }

        // Placeholder para Configuraciones
        btnSettings.setOnClickListener {
            Toast.makeText(this, "Próximamente...", Toast.LENGTH_SHORT).show()
        }

        // Cerrar sesión
        btnLogout.setOnClickListener {
            // Borrar los datos de la sesión
            val session = getSharedPreferences("session", MODE_PRIVATE)
            session.edit().clear().apply()

            // Redirigir a LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
