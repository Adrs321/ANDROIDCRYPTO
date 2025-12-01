package com.example.cryptoapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
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
import kotlinx.coroutines.launch

class NewsActivity : AppCompatActivity() {

    private lateinit var newsListView: ListView
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

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
            result.onSuccess {
                newsListView.adapter = NewsAdapter(this@NewsActivity, it)
                loadingProgressBar.visibility = View.GONE
            }
            result.onFailure {
                Toast.makeText(this@NewsActivity, "Error al cargar las noticias", Toast.LENGTH_SHORT).show()
                loadingProgressBar.visibility = View.GONE
            }
        }
    }
}

// --- Adaptador Personalizado para Noticias ---

class NewsAdapter(context: Context, articles: List<NewsArticle>) :
    ArrayAdapter<NewsArticle>(context, 0, articles) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_news, parent, false)

        val article = getItem(position)!!

        val ivNewsImage = view.findViewById<ImageView>(R.id.iv_news_image)
        val tvTitle = view.findViewById<TextView>(R.id.tv_news_item_title)
        val tvSource = view.findViewById<TextView>(R.id.tv_news_item_source)

        tvTitle.text = article.title
        tvSource.text = article.sourceInfo?.name // ¡CORREGIDO!

        // Cargar imagen con Coil
        ivNewsImage.load(article.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background) // Imagen de placeholder mientras carga
            error(R.drawable.ic_launcher_background) // Imagen si falla la carga
        }

        return view
    }
}
