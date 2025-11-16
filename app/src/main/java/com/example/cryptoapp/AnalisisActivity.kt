package com.example.cryptoapp

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AnalisisActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_analisis)

        val titulo = intent.getStringExtra("TITULO_MONEDA") ?: "Análisis"
        val textoAnalisis = intent.getStringExtra("TEXTO_ANALISIS") ?: "Error al cargar."


        val tvAnalisis: TextView = findViewById(R.id.tv_analisis_completo)


        title = "Análisis: $titulo"
        tvAnalisis.text = textoAnalisis

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}