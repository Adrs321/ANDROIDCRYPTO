package com.example.cryptoapp

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.example.cryptoapp.api.CryptoCoin
import com.example.cryptoapp.api.CryptoRepository
import com.example.cryptoapp.db.CryptoDbHelper
import com.example.cryptoapp.db.CryptoLocalRepository
import kotlinx.coroutines.launch

class FavoriteCoinsActivity : AppCompatActivity() {

    private lateinit var dbHelper: CryptoDbHelper
    private lateinit var favoriteCryptoList: ListView
    private lateinit var tvNoFavorites: TextView

    // Variable correcta para esta clase
    private var userId: Int = -1
    private var favoriteCoins: List<CryptoCoin> = emptyList()

    // Constante necesaria para notificaciones
    private val CHANNEL_ID = "price_alerts_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        dbHelper = CryptoDbHelper(this)
        favoriteCryptoList = findViewById(R.id.lv_favorite_crypto_list)
        tvNoFavorites = findViewById(R.id.tv_no_favorites)

        val session = getSharedPreferences("session", MODE_PRIVATE)
        userId = session.getInt("userId", -1)

        favoriteCryptoList.setOnItemClickListener { _, _, position, _ ->
            val selectedCoin = favoriteCoins[position]
            val intent = Intent(this, CryptoDetailActivity::class.java).apply {
                putExtra("CRYPTO_COIN", selectedCoin)
            }
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1) {
            actualizarDatosYMostrar()
        }
    }

    // --- Lógica de Hardware: Vibración ---
    private fun triggerVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    // --- Función para enviar notificación (Copiada y adaptada) ---
    private fun sendNotification(titulo: String, mensaje: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        try {
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, builder.build())

        } catch (e: Exception) {
            // Manejo de error silencioso
        }
    }

    // --- Lógica de Alertas Corregida ---
    private fun checkAlerts(latestCoins: List<CryptoCoin>) {
        if (userId == -1) return // CORREGIDO: Usamos 'userId'

        val myAlerts = dbHelper.getAlerts(userId) // CORREGIDO: Usamos 'userId'
        var alertTriggered = false

        for (alert in myAlerts) {
            val coin = latestCoins.find { it.symbol.equals(alert.symbol, ignoreCase = true) }

            if (coin != null && coin.priceUsd != null) {
                val precioActual = coin.priceUsd
                val precioObjetivo = alert.targetPrice

                // Lógica inteligente de subida/bajada
                val subioLoSuficiente = alert.isAbove && precioActual >= precioObjetivo
                val bajoLoSuficiente = !alert.isAbove && precioActual <= precioObjetivo

                if (subioLoSuficiente || bajoLoSuficiente) {
                    alertTriggered = true

                    val direccion = if (alert.isAbove) "subió" else "bajó"
                    val mensajeAlerta = "¡${coin.symbol.uppercase()} $direccion a $${alert.targetPrice}!"

                    runOnUiThread {
                        Toast.makeText(this, mensajeAlerta, Toast.LENGTH_LONG).show()
                    }

                    // CORREGIDO: Llamada directa a sendNotification (sin el if 'this is MainActivity')
                    sendNotification("Alerta de Precio", mensajeAlerta)

                    dbHelper.deleteAlert(alert.id)
                }
            }
        }

        if (alertTriggered) {
            triggerVibration()
        }
    }

    private fun actualizarDatosYMostrar() {
        lifecycleScope.launch {
            val resultadoApi = CryptoRepository.fetchCryptoAssets()

            resultadoApi.onSuccess { listaNueva ->
                CryptoLocalRepository.insertMany(this@FavoriteCoinsActivity, listaNueva)
                checkAlerts(listaNueva)
            }

            resultadoApi.onFailure {
                Toast.makeText(this@FavoriteCoinsActivity, "Sin conexión", Toast.LENGTH_SHORT).show()
            }

            loadFavoriteCoins()
        }
    }

    private fun loadFavoriteCoins() {
        favoriteCoins = dbHelper.getFavoriteCoins(userId)

        if (favoriteCoins.isEmpty()) {
            tvNoFavorites.visibility = View.VISIBLE
            favoriteCryptoList.visibility = View.GONE
        } else {
            tvNoFavorites.visibility = View.GONE
            favoriteCryptoList.visibility = View.VISIBLE

            val adapter = ArrayAdapter(
                this,
                R.layout.list_item_crypto,
                R.id.text1,
                favoriteCoins.map { formatCoinForList(it) }
            )
            favoriteCryptoList.adapter = adapter
        }
    }

    private fun formatCoinForList(coin: CryptoCoin): String {
        val price = coin.priceUsd?.let { String.format("%.2f", it) } ?: "N/A"
        val change = coin.changePercent24Hr?.let { String.format("%.2f", it) } ?: "N/A"
        return "${coin.name} (${coin.symbol.uppercase()})\nPrecio: $${price} USD | Cambio 24h: ${change}%"
    }
}