
package com.example.cryptoapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
// Imports de API
import com.example.cryptoapp.api.CryptoCoin
import com.example.cryptoapp.api.CryptoRepository
// Imports de DB
import com.example.cryptoapp.db.CryptoLocalRepository
// Imports de Coroutines
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {


    private lateinit var listaCrypto: ListView
    private lateinit var barraCarga: ProgressBar
    private lateinit var cryptoSearchView: SearchView


    private var listaCompletaDeMonedas: List<CryptoCoin> = emptyList()
    private var listaMostradaDeMonedas: List<CryptoCoin> = emptyList()
    private var adaptadorLista: ArrayAdapter<String>? = null // Referencia al adaptador

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


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

    private fun cargarDatosDeLaDB() {
        lifecycleScope.launch {
            val resultado = CryptoLocalRepository.getAll(this@MainActivity)

            resultado.onSuccess { listaDeMonedasDB ->
                if (listaDeMonedasDB.isNotEmpty()) {
                    listaCompletaDeMonedas = listaDeMonedasDB
                    actualizarListView(listaDeMonedasDB)
                }
            }
            resultado.onFailure { /* No action needed */ }
        }
    }

    private fun refrescarDatosDeLaApi() {
        barraCarga.visibility = View.VISIBLE

        lifecycleScope.launch {
            val resultadoApi = CryptoRepository.fetchCryptoAssets()

            barraCarga.visibility = View.GONE

            resultadoApi.onSuccess { listaDeLaApi ->
                CryptoLocalRepository.insertMany(this@MainActivity, listaDeLaApi)
                // Volver a cargar desde la DB para asegurar consistencia
                cargarDatosDeLaDB()
            }

            resultadoApi.onFailure { error ->
                Toast.makeText(
                    this@MainActivity,
                    "Error de red. Mostrando datos locales.",
                    Toast.LENGTH_SHORT
                ).show()
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
            adaptadorLista = ArrayAdapter(
                this@MainActivity,
                R.layout.list_item_crypto,
                R.id.text1,
                datosFormateados
            )
            listaCrypto.adapter = adaptadorLista
        } else {
            adaptadorLista?.clear()
            adaptadorLista?.addAll(datosFormateados)
            adaptadorLista?.notifyDataSetChanged()
        }
    }
}
