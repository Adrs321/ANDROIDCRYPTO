package com.example.cryptoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cryptoapp.db.CryptoDbHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var dbHelper: CryptoDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verificar si el usuario ya ha iniciado sesión
        val session = getSharedPreferences("session", MODE_PRIVATE)
        if (session.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, Inicio::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        dbHelper = CryptoDbHelper(this)

        val etEmail = findViewById<EditText>(R.id.et_login_email)
        val etPassword = findViewById<EditText>(R.id.et_login_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnGoToRegister = findViewById<Button>(R.id.btn_go_to_register)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.checkUser(email, password)) {
                val userId = dbHelper.getUserId(email)

                // Guardar el estado de la sesión y el ID del usuario
                session.edit().apply {
                    putBoolean("isLoggedIn", true)
                    putInt("userId", userId)
                    apply()
                }
                
                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Inicio::class.java))
                finish()
            } else {
                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
