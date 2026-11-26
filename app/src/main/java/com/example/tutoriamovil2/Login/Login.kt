package com.example.tutoriamovil2.Login

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import com.example.tutoriamovil2.R

class Login : AppCompatActivity() {

    private var enLogin = true
    private var barras = Insets.NONE
    private var teclado = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val contenedor = findViewById<View>(R.id.nav_host_fragment)
        val franja = findViewById<View>(R.id.franjaEstado)
        ViewCompat.setOnApplyWindowInsetsListener(contenedor) { _, insets ->
            barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            teclado = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            ajustarBordes(contenedor, franja)
            insets
        }

        // El login pinta su encabezado azul detrás de la barra de estado;
        // en el registro esa zona la cubre la franja azul.
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHost.navController.addOnDestinationChangedListener { _, destino, _ ->
            enLogin = destino.id == R.id.loginFragment
            ajustarBordes(contenedor, franja)
        }
        // Todas las pantallas de este flujo tienen azul arriba: íconos en blanco.
        WindowCompat.getInsetsController(window, contenedor).isAppearanceLightStatusBars = false
    }

    private fun ajustarBordes(contenedor: View, franja: View) {
        franja.updateLayoutParams { height = if (enLogin) 0 else barras.top }
        contenedor.updatePadding(
            left = barras.left,
            right = barras.right,
            top = 0,
            // Con el teclado abierto se deja su espacio para que el botón siga a la vista.
            bottom = if (enLogin) teclado else maxOf(barras.bottom, teclado),
        )
    }
}
