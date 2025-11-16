package com.example.cryptoapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cryptoapp.api.CryptoCoin
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class CryptoDetailActivity : AppCompatActivity() {

    private lateinit var cryptoCoin: CryptoCoin

    private val generativeModel: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash-lite",
            apiKey = ""
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto_detail)

        cryptoCoin = intent.getParcelableExtra<CryptoCoin>("CRYPTO_COIN")!!

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


        tvName.text = cryptoCoin.name
        tvSymbol.text = "(${cryptoCoin.symbol.uppercase()})"
        tvRank.text = "#${cryptoCoin.marketCapRank?.toString() ?: "N/A"}"
        tvPrice.text = formatCurrency(cryptoCoin.priceUsd)
        tvChange.text = formatPercentage(cryptoCoin.changePercent24Hr)
        tvMarketCap.text = formatLargeNumber(cryptoCoin.marketCap?.toDouble())
        tvVolume.text = formatLargeNumber(cryptoCoin.totalVolume)
        tvSupply.text = formatLargeNumber(cryptoCoin.maxSupply)

        btnAnalyze.setOnClickListener {
            Toast.makeText(this, "Analizando ${cryptoCoin.name}...", Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val analisis = generarAnalisisConIA(cryptoCoin)
                mostrarDialogoDeAnalisis(cryptoCoin.name, analisis)
            }
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
            EN LA RESPUESTA NO DIGAS QUE FALTA INFORMACION
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
