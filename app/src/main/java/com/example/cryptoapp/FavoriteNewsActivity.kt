package com.example.cryptoapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cryptoapp.api.NewsArticle
import com.example.cryptoapp.db.CryptoDbHelper

class FavoriteNewsActivity : AppCompatActivity() {

    private lateinit var newsListView: ListView
    private lateinit var tvEmpty: TextView
    private lateinit var dbHelper: CryptoDbHelper
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news) // Reusamos el layout de noticias

        newsListView = findViewById(R.id.lv_news_list)
        // Puedes agregar un TextView "No tienes noticias favoritas" en el layout si quieres
        // o usar el ProgressBar como placeholder oculto.

        dbHelper = CryptoDbHelper(this)
        val session = getSharedPreferences("session", MODE_PRIVATE)
        userId = session.getInt("userId", -1)

        loadFavoriteNews()

        newsListView.setOnItemClickListener { parent, _, position, _ ->
            val article = parent.adapter.getItem(position) as NewsArticle
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(article.url)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavoriteNews() // Recargar al volver
    }

    private fun loadFavoriteNews() {
        if (userId != -1) {
            val favNews = dbHelper.getFavoriteNews(userId)

            if (favNews.isEmpty()) {
                Toast.makeText(this, "No tienes noticias guardadas", Toast.LENGTH_SHORT).show()
            }

            // Reusamos el NewsAdapter
            newsListView.adapter = NewsAdapter(this, favNews, dbHelper, userId)
        }
    }
}