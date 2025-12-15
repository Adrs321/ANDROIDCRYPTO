package com.example.cryptoapp

import android.Manifest
import android.app.NotificationChannel
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
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
// Imports de API
import com.example.cryptoapp.api.CryptoCoin
import com.example.cryptoapp.api.CryptoRepository
// Imports de DB
import com.example.cryptoapp.db.CryptoDbHelper
import com.example.cryptoapp.db.CryptoLocalRepository
// Imports de Coroutines
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var listaCrypto: ListView
    private lateinit var barraCarga: ProgressBar
    private lateinit var cryptoSearchView: SearchView

    private var listaCompletaDeMonedas: List<CryptoCoin> = emptyList()
    private var listaMostradaDeMonedas: List<CryptoCoin> = emptyList()
    private var adaptadorLista: ArrayAdapter<String>? = null

    private var loggedUserId: Int = -1

    // Constante para el canal de notificaciones
    private val CHANNEL_ID = "price_alerts_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Configurar Canal de Notificaciones (Obligatorio para Android 8+)
        createNotificationChannel()

        // 2. Pedir permiso de notificaciones en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        loggedUserId = intent.getIntExtra("LOGGED_USER_ID", -1)
        if (loggedUserId == -1) {
            val session = getSharedPreferences("session", MODE_PRIVATE)
            loggedUserId = session.getInt("userId", -1)
        }

        listaCrypto = findViewById(R.id.lv_crypto_list)
        barraCarga = findViewById(R.id.pb_loading)
        cryptoSearchView = findViewById(R.id.sv_crypto_search)

        cargarDatosDeLaDB()
        refrescarDatosDeLaApi()

        listaCrypto.setOnItemClickListener { parent, view, position, id ->
            if (position in listaMostradaDeMonedas.indices) {
                val monedaSeleccionada = listaMostradaDeMonedas[position]
                val intent = Intent(this, CryptoDetailActivity::class.java).apply {
                    putExtra("CRYPTO_COIN", monedaSeleccionada)
                }
                startActivity(intent)
            }
        }



        setupSearch()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alertas de Precio"
            val descriptionText = "Notificaciones cuando una cripto llega al precio objetivo"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(titulo: String, mensaje: String) {
        // 1. Verificar Permisos
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return // No hay permiso, salimos
            }
        }

        try {
            // 2. Construir la Notificación
            // Usamos tu propio ícono (ic_launcher_round) para evitar problemas
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            // 3. Enviar usando el Manager nativo del sistema
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Usamos un ID único (tiempo actual) para que las notificaciones no se reemplacen entre sí
            val notificationId = System.currentTimeMillis().toInt()
            notificationManager.notify(notificationId, builder.build())

            android.util.Log.d("NOTIFICACION", "Notificación enviada: $titulo")

        } catch (e: Exception) {
            android.util.Log.e("NOTIFICACION", "Error al enviar: ${e.message}")
        }
    }

    private fun setupSearch() {
        cryptoSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val textoBusqueda = newText.orEmpty()
                val monedasFiltradas = if (textoBusqueda.isEmpty()) {
                    listaCompletaDeMonedas
                } else {
                    listaCompletaDeMonedas.filter { moneda ->
                        moneda.name.contains(textoBusqueda, ignoreCase = true) ||
                                moneda.symbol.contains(textoBusqueda, ignoreCase = true)
                    }
                }
                actualizarListView(monedasFiltradas)
                return true
            }
        })
    }

    private fun triggerVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(500)
        }
    }

    private fun checkAlerts(latestCoins: List<CryptoCoin>) {
        if (loggedUserId == -1) return // O userId en FavoriteCoinsActivity

        val dbHelper = CryptoDbHelper(this) // Asegúrate de usar la importación correcta
        val myAlerts = dbHelper.getAlerts(loggedUserId) // O userId
        var alertTriggered = false

        for (alert in myAlerts) {
            val coin = latestCoins.find { it.symbol.equals(alert.symbol, ignoreCase = true) }

            if (coin != null && coin.priceUsd != null) {
                val precioActual = coin.priceUsd
                val precioObjetivo = alert.targetPrice

                // Lógica Maestra:
                // Caso 1: Esperábamos que SUBA (isAbove=true) y el precio YA ES mayor o igual
                val subioLoSuficiente = alert.isAbove && precioActual >= precioObjetivo

                // Caso 2: Esperábamos que BAJE (isAbove=false) y el precio YA ES menor o igual
                val bajoLoSuficiente = !alert.isAbove && precioActual <= precioObjetivo

                if (subioLoSuficiente || bajoLoSuficiente) {
                    alertTriggered = true

                    val direccion = if (alert.isAbove) "subió" else "bajó"
                    val mensajeAlerta = "¡${coin.symbol.uppercase()} $direccion a $${alert.targetPrice}!"

                    runOnUiThread {
                        Toast.makeText(this, mensajeAlerta, Toast.LENGTH_LONG).show()
                    }

                    // En MainActivity usas sendNotification, en Favorites solo Toast/Vibración
                    // (o copia sendNotification si quieres)
                    if (this is MainActivity) {
                        sendNotification("Alerta de Precio 🔔", mensajeAlerta)
                    }

                    dbHelper.deleteAlert(alert.id)
                }
            }
        }

        if (alertTriggered) {
            triggerVibration()
        }
    }

    private fun cargarDatosDeLaDB() {
        lifecycleScope.launch {
            val resultado = CryptoLocalRepository.getAll(this@MainActivity)
            resultado.onSuccess { listaDeMonedasDB ->
                if (listaDeMonedasDB.isNotEmpty()) {
                    listaCompletaDeMonedas = listaDeMonedasDB
                    actualizarListView(listaDeMonedasDB)
                }
            }
        }
    }

    private fun refrescarDatosDeLaApi() {
        barraCarga.visibility = View.VISIBLE
        lifecycleScope.launch {
            val resultadoApi = CryptoRepository.fetchCryptoAssets()
            barraCarga.visibility = View.GONE

            resultadoApi.onSuccess { listaDeLaApi ->
                CryptoLocalRepository.insertMany(this@MainActivity, listaDeLaApi)
                cargarDatosDeLaDB()
                checkAlerts(listaDeLaApi)
            }

            resultadoApi.onFailure { error ->
                Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun actualizarListView(monedas: List<CryptoCoin>) {
        listaMostradaDeMonedas = monedas
        val datosFormateados = monedas.map { coin ->
            val precio = coin.priceUsd?.let { String.format("%.2f", it) } ?: "N/A"
            val cambio = coin.changePercent24Hr?.let { String.format("%.2f", it) } ?: "N/A"
            "${coin.name} (${coin.symbol.uppercase()})\nPrecio: $${precio} USD | Cambio 24h: ${cambio}%"
        }

        if (adaptadorLista == null) {
            adaptadorLista = ArrayAdapter(this@MainActivity, R.layout.list_item_crypto, R.id.text1, datosFormateados)
            listaCrypto.adapter = adaptadorLista
        } else {
            adaptadorLista?.clear()
            adaptadorLista?.addAll(datosFormateados)
            adaptadorLista?.notifyDataSetChanged()
        }
    }
}