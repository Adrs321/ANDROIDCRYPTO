package com.example.cryptoapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.cryptoapp.api.*
import com.example.cryptoapp.db.CryptoDbHelper
import kotlinx.coroutines.launch

class NewsActivity : AppCompatActivity() {

    private lateinit var newsListView: ListView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var dbHelper: CryptoDbHelper
    private var userId: Int = -1
    private var userName: String = "Anónimo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)

        dbHelper = CryptoDbHelper(this)
        val session = getSharedPreferences("session", MODE_PRIVATE)
        userId = session.getInt("userId", -1)

        // Obtenemos el nombre del usuario para guardarlo en MockAPI
        if (userId != -1) {
            val userData = dbHelper.getUserData(userId)
            userName = userData?.first ?: "Usuario $userId"
        }

        newsListView = findViewById(R.id.lv_news_list)
        loadingProgressBar = findViewById(R.id.pb_news_loading)

        fetchNews()

        // Click en la noticia (abrir web)
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
                // Pasamos una función lambda para manejar el click en comentarios
                val adapter = NewsAdapter(this@NewsActivity, articles, dbHelper, userId) { article ->
                    abrirDialogoComentarios(article)
                }
                newsListView.adapter = adapter
                loadingProgressBar.visibility = View.GONE
            }
            result.onFailure {
                Toast.makeText(this@NewsActivity, "Error al cargar noticias", Toast.LENGTH_SHORT).show()
                loadingProgressBar.visibility = View.GONE
            }
        }
    }

    // --- LÓGICA CRUD CON MOCKAPI ---
    private fun abrirDialogoComentarios(article: NewsArticle) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_comments, null)
        val lvComments = dialogView.findViewById<ListView>(R.id.lv_comments)
        val etInput = dialogView.findViewById<EditText>(R.id.et_comment_input)
        val btnSend = dialogView.findViewById<ImageButton>(R.id.btn_send_comment)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)

        tvTitle.text = "Comentarios"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setNegativeButton("Cerrar", null)
            .create()

        // Función para cargar comentarios desde la Nube (READ)
        fun cargarComentarios() {
            lifecycleScope.launch {
                try {
                    val comments = CommentsApiClient.service.getComments(article.id)

                    // Adaptador simple para mostrar la lista
                    val adapter = object : ArrayAdapter<Comment>(this@NewsActivity, android.R.layout.simple_list_item_2, android.R.id.text1, comments) {
                        override fun getView(pos: Int, convView: View?, parent: ViewGroup): View {
                            val v = super.getView(pos, convView, parent)
                            val t1 = v.findViewById<TextView>(android.R.id.text1)
                            val t2 = v.findViewById<TextView>(android.R.id.text2)

                            val item = getItem(pos)!!
                            t1.text = "${item.userName}: ${item.text}"
                            t1.setTextColor(Color.WHITE)

                            // Si el comentario es mío, muestro indicación
                            if (item.userId == userId) {
                                t2.text = "Toca para Editar/Borrar"
                                t2.setTextColor(Color.CYAN)
                            } else {
                                t2.text = ""
                            }
                            return v
                        }
                    }
                    lvComments.adapter = adapter

                } catch (e: Exception) {
                    Toast.makeText(this@NewsActivity, "Error al cargar comentarios", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cargarComentarios()

        // 1. CREAR (CREATE)
        btnSend.setOnClickListener {
            val texto = etInput.text.toString()
            if (texto.isNotEmpty()) {
                val nuevoComentario = Comment(
                    newsId = article.id,
                    userId = userId,
                    userName = userName,
                    text = texto,
                    date = System.currentTimeMillis()
                )

                lifecycleScope.launch {
                    try {
                        CommentsApiClient.service.createComment(nuevoComentario)
                        etInput.setText("")
                        cargarComentarios() // Recargar lista
                        Toast.makeText(this@NewsActivity, "Comentario enviado", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@NewsActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Click en un comentario (para Editar o Borrar)
        lvComments.setOnItemClickListener { _, _, position, _ ->
            val comment = lvComments.adapter.getItem(position) as Comment

            // Solo puedo editar MIS comentarios
            if (comment.userId == userId) {
                val opciones = arrayOf("Editar", "Eliminar")
                AlertDialog.Builder(this)
                    .setTitle("Gestionar Comentario")
                    .setItems(opciones) { _, which ->
                        if (which == 0) {
                            // 2. EDITAR (UPDATE)
                            val inputEdit = EditText(this)
                            inputEdit.setText(comment.text)
                            AlertDialog.Builder(this)
                                .setTitle("Editar")
                                .setView(inputEdit)
                                .setPositiveButton("Guardar") { _, _ ->
                                    val comentarioEditado = comment.copy(text = inputEdit.text.toString())
                                    lifecycleScope.launch {
                                        try {
                                            // MockAPI requiere ID string
                                            CommentsApiClient.service.updateComment(comment.id!!, comentarioEditado)
                                            cargarComentarios()
                                        } catch (e: Exception) {
                                            Toast.makeText(this@NewsActivity, "Error al editar", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .show()
                        } else {
                            // 3. ELIMINAR (DELETE)
                            lifecycleScope.launch {
                                try {
                                    CommentsApiClient.service.deleteComment(comment.id!!)
                                    cargarComentarios()
                                    Toast.makeText(this@NewsActivity, "Eliminado", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(this@NewsActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    .show()
            }
        }

        dialog.show()
    }
}

// --- ADAPTADOR (Con soporte para click en comentario) ---

class NewsAdapter(
    context: Context,
    articles: List<NewsArticle>,
    private val dbHelper: CryptoDbHelper,
    private val userId: Int,
    private val onCommentClick: (NewsArticle) -> Unit // Callback para abrir diálogo
) : ArrayAdapter<NewsArticle>(context, 0, articles) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_news, parent, false)
        val article = getItem(position)!!

        val ivNewsImage = view.findViewById<ImageView>(R.id.iv_news_image)
        val tvTitle = view.findViewById<TextView>(R.id.tv_news_item_title)
        val tvSource = view.findViewById<TextView>(R.id.tv_news_item_source)
        val btnFav = view.findViewById<ImageButton>(R.id.btn_fav_news)
        val btnComment = view.findViewById<ImageButton>(R.id.btn_comment_news)

        tvTitle.text = article.title
        tvSource.text = article.sourceInfo?.name

        ivNewsImage.load(article.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        if (userId != -1) {
            // Favoritos
            btnFav.visibility = View.VISIBLE
            var isFav = dbHelper.isFavoriteNews(userId, article.id)
            updateFavIcon(btnFav, isFav)

            btnFav.setOnClickListener {
                if (isFav) dbHelper.removeFavoriteNews(userId, article.id)
                else dbHelper.addFavoriteNews(userId, article)
                isFav = !isFav
                updateFavIcon(btnFav, isFav)
            }
            btnFav.isFocusable = false; btnFav.isFocusableInTouchMode = false

            // Comentarios
            btnComment.visibility = View.VISIBLE
            btnComment.setOnClickListener {
                onCommentClick(article) // Llamamos a la Activity
            }
            btnComment.isFocusable = false; btnComment.isFocusableInTouchMode = false

        } else {
            btnFav.visibility = View.GONE
            btnComment.visibility = View.GONE
        }

        return view
    }

    private fun updateFavIcon(btn: ImageButton, isFav: Boolean) {
        if (isFav) {
            btn.setImageResource(android.R.drawable.star_on)
            btn.setColorFilter(Color.parseColor("#FFC107"), PorterDuff.Mode.SRC_IN)
        } else {
            btn.setImageResource(android.R.drawable.star_off)
            btn.setColorFilter(Color.parseColor("#B0B0B0"), PorterDuff.Mode.SRC_IN)
        }
    }
}