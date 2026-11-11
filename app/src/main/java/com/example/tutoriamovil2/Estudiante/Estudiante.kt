package com.example.tutoriamovil2.Estudiante

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.red.EventosSesion
import com.google.android.material.bottomnavigation.BottomNavigationView

class Estudiante : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_estudiante)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ConstraintLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_estudiante) as NavHostFragment
        findViewById<BottomNavigationView>(R.id.bottomNavEstudiante)
            .setupWithNavController(navHost.navController)
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