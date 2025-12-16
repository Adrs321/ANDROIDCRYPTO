package com.example.cryptoapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cryptoapp.api.Comment
import com.example.cryptoapp.api.CommentsApiClient
import com.example.cryptoapp.api.NewsArticle
import com.example.cryptoapp.db.CryptoDbHelper
import kotlinx.coroutines.launch

class FavoriteNewsActivity : AppCompatActivity() {

    private lateinit var newsListView: ListView
    private lateinit var tvEmpty: TextView
    private lateinit var dbHelper: CryptoDbHelper
    private var userId: Int = -1
    private var userName: String = "Anónimo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news) // Reusamos el layout de noticias

        newsListView = findViewById(R.id.lv_news_list)
        // Opcional: Si tienes un TextView para "vacío" en tu layout
        // tvEmpty = findViewById(R.id.tv_empty)

        dbHelper = CryptoDbHelper(this)
        val session = getSharedPreferences("session", MODE_PRIVATE)
        userId = session.getInt("userId", -1)

        // Obtener nombre del usuario para los comentarios
        if (userId != -1) {
            val userData = dbHelper.getUserData(userId)
            userName = userData?.first ?: "Usuario $userId"
        }

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

            // --- AQUÍ ESTABA EL ERROR ---
            // Ahora pasamos la función lambda para manejar el click en comentarios
            val adapter = NewsAdapter(this, favNews, dbHelper, userId) { article ->
                abrirDialogoComentarios(article)
            }
            newsListView.adapter = adapter
        }
    }

    // --- COPIAMOS LA LÓGICA DE COMENTARIOS PARA QUE FUNCIONE AQUÍ TAMBIÉN ---
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

        fun cargarComentarios() {
            lifecycleScope.launch {
                try {
                    val comments = CommentsApiClient.service.getComments(article.id)

                    val adapter = object : ArrayAdapter<Comment>(this@FavoriteNewsActivity, android.R.layout.simple_list_item_2, android.R.id.text1, comments) {
                        override fun getView(pos: Int, convView: View?, parent: ViewGroup): View {
                            val v = super.getView(pos, convView, parent)
                            val t1 = v.findViewById<TextView>(android.R.id.text1)
                            val t2 = v.findViewById<TextView>(android.R.id.text2)

                            val item = getItem(pos)!!
                            t1.text = "${item.userName}: ${item.text}"
                            t1.setTextColor(Color.WHITE)

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
                    // Silencioso o Toast
                }
            }
        }

        cargarComentarios()

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
                        cargarComentarios()
                    } catch (e: Exception) {
                        Toast.makeText(this@FavoriteNewsActivity, "Error al enviar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        lvComments.setOnItemClickListener { _, _, position, _ ->
            val comment = lvComments.adapter.getItem(position) as Comment
            if (comment.userId == userId) {
                val opciones = arrayOf("Editar", "Eliminar")
                AlertDialog.Builder(this)
                    .setTitle("Gestionar")
                    .setItems(opciones) { _, which ->
                        if (which == 0) {
                            val inputEdit = EditText(this)
                            inputEdit.setText(comment.text)
                            AlertDialog.Builder(this)
                                .setView(inputEdit)
                                .setPositiveButton("Guardar") { _, _ ->
                                    val editado = comment.copy(text = inputEdit.text.toString())
                                    lifecycleScope.launch {
                                        try {
                                            CommentsApiClient.service.updateComment(comment.id!!, editado)
                                            cargarComentarios()
                                        } catch (e: Exception) {}
                                    }
                                }
                                .show()
                        } else {
                            lifecycleScope.launch {
                                try {
                                    CommentsApiClient.service.deleteComment(comment.id!!)
                                    cargarComentarios()
                                } catch (e: Exception) {}
                            }
                        }
                    }
                    .show()
            }
        }
        dialog.show()
    }
}