package com.example.tutoriamovil2.Administrador

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.base.prepararMarcoConBarra
import com.example.tutoriamovil2.red.EventosSesion
import com.google.android.material.bottomnavigation.BottomNavigationView

/** Panel del administrador (Bienestar): inicio, no realizadas, aprobaciones y bitácora. */
class Administrador : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_administrador)
        val barraInferior = findViewById<BottomNavigationView>(R.id.bottomNavAdministrador)
        prepararMarcoConBarra(
            raiz = findViewById(R.id.ConstraintLayout),
            franja = findViewById(R.id.franjaEstado),
            barraInferior = barraInferior,
        )

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_administrador) as NavHostFragment
        barraInferior.setupWithNavController(navHost.navController)
    }

    override fun onResume() {
        super.onResume()
        // Si el servidor responde 401, se vuelve al login.
        EventosSesion.alCaducar = { runOnUiThread { volverAlLogin() } }
    }

    override fun onPause() {
        super.onPause()
        EventosSesion.alCaducar = null
    }

    private fun volverAlLogin() {
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
