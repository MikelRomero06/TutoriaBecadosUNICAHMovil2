package com.example.tutoriamovil2

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.tutoriamovil2.Login.Login

class PantallaPresentacion : AppCompatActivity() {

    private val duracionSplash = 1200L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pantalla_presentacion)
        val raiz = findViewById<android.view.View>(R.id.ConstraintLayout)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        raiz.postDelayed({
            startActivity(Intent(this, Login::class.java))
            finish()
        }, duracionSplash)
    }
}