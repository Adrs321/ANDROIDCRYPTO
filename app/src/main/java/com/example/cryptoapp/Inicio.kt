package com.example.cryptoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class Inicio : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)


        val botonSiguiente = findViewById<Button>(R.id.Siguiente)


        botonSiguiente.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            

            startActivity(intent)
        }
    }
}
