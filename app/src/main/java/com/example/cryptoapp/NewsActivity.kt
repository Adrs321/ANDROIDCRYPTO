package com.example.cryptoapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.cryptoapp.api.NewsArticle
import com.example.cryptoapp.api.NoticiasRepository
import com.example.cryptoapp.db.CryptoDbHelper
import kotlinx.coroutines.launch

class NewsActivity : AppCompatActivity() {

    private lateinit var newsListView: ListView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var dbHelper: CryptoDbHelper // Referencia a la DB
    private var userId: Int = -1 // ID del usuario actual

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        // 1. Inicializar DB y obtener Usuario
        dbHelper = CryptoDbHelper(this)
        val session = getSharedPreferences("session", MODE_PRIVATE)
        userId = session.getInt("userId", -1)

        newsListView = findViewById(R.id.lv_news_list)
        loadingProgressBar = findViewById(R.id.pb_news_loading)

        fetchNews()

        newsListView.setOnItemClickListener { parent, _, position, _ ->
            val article = parent.adapter.getItem(position) as NewsArticle
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(article.url)
            }
            startActivity(intent)
        }
    }

    private fun fetchNews() {
        loadingProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = NoticiasRepository.fetchNews()
            result.onSuccess { articles ->
                // 2. Pasamos dbHelper y userId al adaptador actualizado
                newsListView.adapter = NewsAdapter(this@NewsActivity, articles, dbHelper, userId)
                loadingProgressBar.visibility = View.GONE
            }
            result.onFailure {
                Toast.makeText(this@NewsActivity, "Error al cargar las noticias", Toast.LENGTH_SHORT).show()
                loadingProgressBar.visibility = View.GONE
            }
        }
    }
}

// --- Adaptador Personalizado Actualizado ---

class NewsAdapter(
    context: Context,
    articles: List<NewsArticle>,
    private val dbHelper: CryptoDbHelper, // Recibimos la DB para verificar favoritos
    private val userId: Int               // Recibimos el ID del usuario
) : ArrayAdapter<NewsArticle>(context, 0, articles) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_news, parent, false)

        val article = getItem(position)!!

        val ivNewsImage = view.findViewById<ImageView>(R.id.iv_news_image)
        val tvTitle = view.findViewById<TextView>(R.id.tv_news_item_title)
        val tvSource = view.findViewById<TextView>(R.id.tv_news_item_source)

        // Buscamos el botón de favorito (Asegúrate de haber actualizado list_item_news.xml)
        val btnFav = view.findViewById<ImageButton>(R.id.btn_fav_news)

        tvTitle.text = article.title
        tvSource.text = article.sourceInfo?.name

        // Cargar imagen con Coil
        ivNewsImage.load(article.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        // --- LÓGICA DE FAVORITOS ---
        if (userId != -1) {
            btnFav.visibility = View.VISIBLE

            // Verificamos si ya es favorita
            var isFav = dbHelper.isFavoriteNews(userId, article.id)
            updateFavIcon(btnFav, isFav)

            btnFav.setOnClickListener {
                if (isFav) {
                    dbHelper.removeFavoriteNews(userId, article.id)
                    Toast.makeText(context, "Noticia eliminada de favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    dbHelper.addFavoriteNews(userId, article)
                    Toast.makeText(context, "Noticia guardada en favoritos", Toast.LENGTH_SHORT).show()
                }
                // Invertimos el estado y actualizamos icono
                isFav = !isFav
                updateFavIcon(btnFav, isFav)
            }

            // Evitar que el click en el botón active el click de la lista (abrir noticia)
            btnFav.isFocusable = false
            btnFav.isFocusableInTouchMode = false
        } else {
            // Si no hay usuario logueado, ocultamos la estrella
            btnFav.visibility = View.GONE
        }

        return view
    }

    private fun updateFavIcon(btn: ImageButton, isFav: Boolean) {
        if (isFav) {
            btn.setImageResource(android.R.drawable.star_on)
            // Color Amarillo para activado
            btn.setColorFilter(Color.parseColor("#FFC107"), PorterDuff.Mode.SRC_IN)
        } else {
            btn.setImageResource(android.R.drawable.star_off)
            // Color Gris para desactivado
            btn.setColorFilter(Color.parseColor("#B0B0B0"), PorterDuff.Mode.SRC_IN)
        }
    }
}