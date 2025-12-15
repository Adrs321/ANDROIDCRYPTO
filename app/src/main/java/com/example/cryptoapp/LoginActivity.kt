package com.example.cryptoapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.cryptoapp.db.CryptoDbHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

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

        // Referencias a la UI
        val etEmail = findViewById<EditText>(R.id.et_login_email)
        val etPassword = findViewById<EditText>(R.id.et_login_password)
        val btnLogin = findViewById<Button>(R.id.btn_login)
        val btnGoToRegister = findViewById<Button>(R.id.btn_go_to_register)

        // Botón de Google (Asegúrate de haberlo agregado en el XML)
        val btnGoogle = findViewById<Button>(R.id.btn_google_login)

        // --- 1. Configuración de Google Sign-In ---
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- 2. Launcher para el resultado de Google ---
        val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Si la autenticación es exitosa, procesamos el usuario
                handleGoogleSignIn(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Error con Google: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }

        // --- 3. Listeners ---

        // Login Manual
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.checkUser(email, password)) {
                val userId = dbHelper.getUserId(email)
                loginSuccess(userId)
            } else {
                Toast.makeText(this, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        // Login con Google
        btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signInIntent)
        }

        // Ir a Registro
        btnGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // --- Lógica para manejar la cuenta de Google ---
    private fun handleGoogleSignIn(account: GoogleSignInAccount) {
        val email = account.email ?: return
        val name = account.displayName ?: "Usuario Google"
        val googleId = account.id ?: "google_default"

        // 1. Verificamos si el usuario ya existe en la DB por su email
        val existingUserId = dbHelper.getUserId(email)

        if (existingUserId != -1) {
            // EL USUARIO YA EXISTE: Iniciamos sesión directamente
            loginSuccess(existingUserId)
        } else {
            // EL USUARIO NO EXISTE: Lo registramos automáticamente
            // Usamos el ID de Google como contraseña interna
            val res = dbHelper.addUser(name, email, googleId)

            if (res != -1L) {
                // Registro exitoso, ahora obtenemos su ID y entramos
                val newUserId = dbHelper.getUserId(email)
                Toast.makeText(this, "Cuenta creada con Google", Toast.LENGTH_SHORT).show()
                loginSuccess(newUserId)
            } else {
                Toast.makeText(this, "Error al registrar usuario de Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Función auxiliar para guardar sesión e ir a Inicio ---
    private fun loginSuccess(userId: Int) {
        val session = getSharedPreferences("session", MODE_PRIVATE)
        session.edit().apply {
            putBoolean("isLoggedIn", true)
            putInt("userId", userId)
            apply()
        }

        Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, Inicio::class.java))
        finish()
    }
}