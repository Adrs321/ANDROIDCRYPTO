package com.example.cryptoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog // Importante para el diálogo
import androidx.appcompat.app.AppCompatActivity
import com.example.cryptoapp.db.CryptoDbHelper // Importante para la DB

class Inicio : AppCompatActivity() {

    // Declaramos el helper
    private lateinit var dbHelper: CryptoDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        // Inicializamos la DB
        dbHelper = CryptoDbHelper(this)

        val btnCoinList = findViewById<Button>(R.id.btn_coin_list)
        val btnFavoriteCoins = findViewById<Button>(R.id.btn_favorite_coins)
        val btnNews = findViewById<Button>(R.id.btn_news)

        // CAMBIO: Ahora buscamos btn_account
        val btnAccount = findViewById<Button>(R.id.btn_account)

        val btnLogout = findViewById<Button>(R.id.btn_logout)

        btnCoinList.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        btnFavoriteCoins.setOnClickListener {
            startActivity(Intent(this, FavoriteCoinsActivity::class.java))
        }

        btnNews.setOnClickListener {
            startActivity(Intent(this, NewsActivity::class.java))
        }

        val btnFavNews = findViewById<Button>(R.id.btn_fav_news)

        btnFavNews.setOnClickListener {
            startActivity(Intent(this, FavoriteNewsActivity::class.java))
        }

        // --- NUEVA LÓGICA: MI CUENTA ---
        btnAccount.setOnClickListener {
            mostrarInformacionDeCuenta()
        }

        btnLogout.setOnClickListener {
            val session = getSharedPreferences("session", MODE_PRIVATE)
            session.edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun mostrarInformacionDeCuenta() {
        // 1. Obtener ID de la sesión
        val session = getSharedPreferences("session", MODE_PRIVATE)
        val userId = session.getInt("userId", -1)

        if (userId != -1) {
            // 2. Buscar datos en la DB
            val userData = dbHelper.getUserData(userId)

            if (userData != null) {
                val (nombre, email) = userData

                // 3. Mostrar Diálogo
                AlertDialog.Builder(this)
                    .setTitle("👤 Mi Cuenta")
                    .setMessage("Nombre: $nombre\n\nCorreo: $email")
                    .setPositiveButton("Cerrar") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            } else {
                // Caso raro: ID existe en sesión pero no en DB
                AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("No se encontraron los datos del usuario.")
                    .setPositiveButton("Ok", null)
                    .show()
            }
        }
    }
}