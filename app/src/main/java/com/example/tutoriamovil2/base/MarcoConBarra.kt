package com.example.tutoriamovil2.base

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding

/**
 * Marco común de las pantallas con barra inferior (estudiante, tutor y admin):
 * una franja azul detrás de la barra de estado y la barra inferior hasta el borde.
 */
fun Activity.prepararMarcoConBarra(raiz: View, franja: View, barraInferior: View) {
    ViewCompat.setOnApplyWindowInsetsListener(raiz) { v, insets ->
        val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(barras.left, 0, barras.right, 0)
        franja.updateLayoutParams { height = barras.top }
        insets
    }
    // Este listener reemplaza al de Material, que si no borraría el padding.
    ViewCompat.setOnApplyWindowInsetsListener(barraInferior) { v, insets ->
        v.updatePadding(bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
        insets
    }
    // Sobre la franja azul, los íconos de la barra de estado van en blanco.
    WindowCompat.getInsetsController(window, raiz).isAppearanceLightStatusBars = false
}
