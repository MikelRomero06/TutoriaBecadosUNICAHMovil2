package com.example.tutoriamovil2

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.base.entrarConRebote
import com.example.tutoriamovil2.base.entrarDesdeAbajo

class PantallaPresentacion : AppCompatActivity() {

    private val duracionSplash = 1600L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pantalla_presentacion)
        val raiz = findViewById<View>(R.id.ConstraintLayout)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Fondo azul: íconos de la barra de estado en blanco.
        WindowCompat.getInsetsController(window, raiz).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        findViewById<View>(R.id.contenedorLogo).entrarConRebote()
        findViewById<View>(R.id.tvNombre).entrarDesdeAbajo(retraso = 180)
        findViewById<View>(R.id.tvLema).entrarDesdeAbajo(retraso = 300)
        findViewById<View>(R.id.pie).entrarDesdeAbajo(retraso = 420, distanciaDp = 12f)

        raiz.postDelayed({
            startActivity(Intent(this, Login::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, duracionSplash)
    }
}
