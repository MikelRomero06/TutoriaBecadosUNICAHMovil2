package com.example.tutoriamovil2.Estudiante

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tutoriamovil2.Login.Login
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.red.EventosSesion

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