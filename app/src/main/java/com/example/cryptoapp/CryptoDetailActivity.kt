package com.example.cryptoapp

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
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

    // Configuración del modelo Gemini
    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            // Usamos 1.5-flash que es rápido y estable para cuentas gratuitas
            modelName = "gemini-2.0-flash-lite",
            apiKey = "AIzaSyDxMQuw_GQkzszFBPeNrwh93B78tFpP6b8" // Tu API Key
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto_detail)

        dbHelper = CryptoDbHelper(this)

        // Recuperamos la moneda del Intent
        cryptoCoin = intent.getParcelableExtra<CryptoCoin>("CRYPTO_COIN")!!

        // Obtener el ID del usuario de la sesión
        val session = getSharedPreferences("session", MODE_PRIVATE)
        userId = session.getInt("userId", -1)

        // Bind views (Enlazar vistas)
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


        val btnAlert: ImageButton = findViewById(R.id.btn_alert)

        // Set data (Poblar datos)
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
            // Verificar si es favorita al iniciar
            isFavorite = dbHelper.isFavorite(userId, cryptoCoin.id)
            updateFavoriteButton(btnFavorite)

            // Click en Favorito
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

            // --- NUEVO: Click en Alerta ---
            btnAlert.setOnClickListener {
                mostrarDialogoCrearAlerta()
            }
        } else {
            // Si no hay sesión iniciada, avisar al usuario
            btnFavorite.setOnClickListener {
                Toast.makeText(this, "Inicia sesión para guardar favoritos", Toast.LENGTH_SHORT).show()
            }
            btnAlert.setOnClickListener {
                Toast.makeText(this, "Inicia sesión para crear alertas", Toast.LENGTH_SHORT).show()
            }
        }

        // Lógica de Análisis con IA
        btnAnalyze.setOnClickListener {
            Toast.makeText(this, "Analizando ${cryptoCoin.name}...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val analisis = generarAnalisisConIA(cryptoCoin)
                mostrarDialogoDeAnalisis(cryptoCoin.name, analisis)
            }
        }
    }

    // --- Funciones Auxiliares ---

    private fun updateFavoriteButton(btnFavorite: ImageButton) {
        if (isFavorite) {
            btnFavorite.setImageResource(android.R.drawable.star_on)
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#FFC107"), android.graphics.PorterDuff.Mode.SRC_IN)
        } else {
            btnFavorite.setImageResource(android.R.drawable.star_off)
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

        // Prompt actualizado para pedir Sentimiento
        val prompt = """
            Eres un experto analista de criptomonedas.
            Analiza: $nombre (${moneda.symbol}).
            Precio Actual: $precio.
            Cambio 24h: $cambio.
            
            Tu tarea:
            1. Indica el "Sentimiento de Mercado" actual. Usa ESTRICTAMENTE una de estas etiquetas al inicio:
               "SENTIMIENTO: ALCISTA (Bullish)"
               "SENTIMIENTO: BAJISTA (Bearish)"
               "SENTIMIENTO: NEUTRAL"
            
            2. Escribe un análisis técnico muy breve (máximo 3 líneas) explicando por qué.
            3. Termina con la recomendación en mayúsculas: (COMPRAR / VENDER / MANTENER).
            
            No menciones que te falta información. Usa los datos provistos.
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


    private fun mostrarDialogoCrearAlerta() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("🔔 Crear Alerta de Precio")
        builder.setMessage("Avísame cuando ${cryptoCoin.name} llegue a:")

        // Input para el precio
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Ej: 65000.50"

        // Pre-llenar con el precio actual (sin el símbolo $)
        // Usamos let para asegurarnos de que no sea nulo
        val precioActual = cryptoCoin.priceUsd ?: 0.0
        if (precioActual > 0) {
            input.setText(precioActual.toString())
        }

        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val textoPrecio = input.text.toString()

            if (textoPrecio.isNotEmpty()) {
                val precioObjetivo = textoPrecio.toDoubleOrNull()
                // Obtenemos el precio actual (seguro porque si estamos aquí, la moneda cargó)
                val precioActual = cryptoCoin.priceUsd ?: 0.0

                if (precioObjetivo != null && precioActual > 0) {
                    // CAMBIO: Pasamos precioObjetivo Y precioActual
                    dbHelper.addAlert(userId, cryptoCoin.symbol, precioObjetivo, precioActual)

                    val tipoAlerta = if (precioObjetivo > precioActual) "suba" else "baje"
                    Toast.makeText(this, "Avisar cuando $tipoAlerta a $${precioObjetivo}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show()
                }
            }
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }
}