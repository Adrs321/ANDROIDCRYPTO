package com.example.cryptoapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cryptoapp.api.CryptoCoin
import com.example.cryptoapp.db.CryptoDbHelper
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class CryptoDetailActivity : AppCompatActivity() {

    private lateinit var cryptoCoin: CryptoCoin
    private lateinit var dbHelper: CryptoDbHelper
    private var userId: Int = -1
    private var isFavorite = false

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = "AIzaSyBK8_VAAqWb2a1h2ZOHS8syRdlO9ro4-xk"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto_detail)

        dbHelper = CryptoDbHelper(this)
        cryptoCoin = intent.getParcelableExtra<CryptoCoin>("CRYPTO_COIN")!!

        // Obtener el ID del usuario de la sesión
        val session = getSharedPreferences("session", MODE_PRIVATE)
        userId = session.getInt("userId", -1)

        // Bind views
        val tvName: TextView = findViewById(R.id.tv_detail_name)
        val tvSymbol: TextView = findViewById(R.id.tv_detail_symbol)
        val tvPrice: TextView = findViewById(R.id.tv_detail_price)
        val tvChange: TextView = findViewById(R.id.tv_detail_change)
        val tvRank: TextView = findViewById(R.id.tv_detail_rank)
        val tvMarketCap: TextView = findViewById(R.id.tv_detail_market_cap)
        val tvVolume: TextView = findViewById(R.id.tv_detail_volume)
        val tvSupply: TextView = findViewById(R.id.tv_detail_supply)
        val btnAnalyze: Button = findViewById(R.id.btn_analyze_ia)
        val btnFavorite: ImageButton = findViewById(R.id.btn_favorite)

        // Set data
        tvName.text = cryptoCoin.name
        tvSymbol.text = "(${cryptoCoin.symbol.uppercase()})"
        tvRank.text = "#${cryptoCoin.marketCapRank?.toString() ?: "N/A"}"
        tvPrice.text = formatCurrency(cryptoCoin.priceUsd)
        tvChange.text = formatPercentage(cryptoCoin.changePercent24Hr)
        tvMarketCap.text = formatLargeNumber(cryptoCoin.marketCap?.toDouble())
        tvVolume.text = formatLargeNumber(cryptoCoin.totalVolume)
        tvSupply.text = formatLargeNumber(cryptoCoin.maxSupply)

        // Lógica de Favoritos
        if (userId != -1) {
            isFavorite = dbHelper.isFavorite(userId, cryptoCoin.id)
            updateFavoriteButton(btnFavorite)

            btnFavorite.setOnClickListener {
                if (isFavorite) {
                    dbHelper.removeFavorite(userId, cryptoCoin.id)
                    Toast.makeText(this, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    dbHelper.addFavorite(userId, cryptoCoin.id)
                    Toast.makeText(this, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                }
                isFavorite = !isFavorite
                updateFavoriteButton(btnFavorite)
            }
        }

        btnAnalyze.setOnClickListener {
            Toast.makeText(this, "Analizando ${cryptoCoin.name}...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val analisis = generarAnalisisConIA(cryptoCoin)
                mostrarDialogoDeAnalisis(cryptoCoin.name, analisis)
            }
        }
    }

    private fun updateFavoriteButton(btnFavorite: ImageButton) {
        if (isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.star_on)
            // Amarillo ámbar para la estrella marcada
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#FFC107"), android.graphics.PorterDuff.Mode.SRC_IN)
        } else {
            btnFavorite.setImageResource(android.R.drawable.star_off)
            // Gris claro para la estrella sin marcar
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#B0B0B0"), android.graphics.PorterDuff.Mode.SRC_IN)
        }
    }

    private fun formatCurrency(value: Double?): String {
        return if (value != null) {
            NumberFormat.getCurrencyInstance(Locale.US).format(value)
        } else {
            "N/A"
        }
    }

    private fun formatPercentage(value: Double?): String {
        return if (value != null) {
            String.format("%.2f%%", value)
        } else {
            "N/A"
        }
    }

    private fun formatLargeNumber(value: Double?): String {
        if (value == null) return "N/A"
        return when {
            value >= 1_000_000_000_000 -> String.format("%.2fT", value / 1_000_000_000_000)
            value >= 1_000_000_000 -> String.format("%.2fB", value / 1_000_000_000)
            value >= 1_000_000 -> String.format("%.2fM", value / 1_000_000)
            else -> NumberFormat.getInstance().format(value)
        }
    }

    private suspend fun generarAnalisisConIA(moneda: CryptoCoin): String {
        val precio = formatCurrency(moneda.priceUsd)
        val cambio = formatPercentage(moneda.changePercent24Hr)
        val nombre = moneda.name

        val prompt = """
            Eres un asistente de trading de criptomonedas.
            Un usuario está viendo la moneda: $nombre (${moneda.symbol}).
            Su precio actual es $precio.
            Su cambio en las últimas 24 horas es de $cambio.

            Basado *solo* en esta información, dame un breve análisis técnico
            (de 2 a 3 líneas) y una recomendación final en mayúsculas: 
            (COMPRAR, VENDER, o MANTENER).
            EN LA RESPUESTA NUNCA DIGAS O MENCIONES DE QUE TE FALTA INFORMACION PARA PODER HACER EL ANALISIS.
        """.trimIndent()

        return try {
            val response = withContext(Dispatchers.IO) {
                generativeModel.generateContent(prompt)
            }
            response.text ?: "Error: No se pudo generar respuesta."
        } catch (e: Exception) {
            "Error al contactar la IA: ${e.message}"
        }
    }

    private fun mostrarDialogoDeAnalisis(tituloMoneda: String, analisis: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Análisis IA: $tituloMoneda")
        builder.setMessage(analisis)
        builder.setPositiveButton("Aceptar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }
}
