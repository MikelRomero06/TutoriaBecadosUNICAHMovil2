package com.example.tutoriamovil2.base

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.DecelerateInterpolator

// Animaciones cortas que se repiten en varias pantallas.
// Todas respetan la escala de animación del sistema: si el usuario las apaga, no se ven.

private fun View.dp(valor: Float) = valor * resources.displayMetrics.density

/** Aparece subiendo un poco. */
fun View.entrarDesdeAbajo(retraso: Long = 0, distanciaDp: Float = 20f, duracion: Long = 420) {
    alpha = 0f
    translationY = dp(distanciaDp)
    animate()
        .alpha(1f)
        .translationY(0f)
        .setStartDelay(retraso)
        .setDuration(duracion)
        .setInterpolator(DecelerateInterpolator(1.6f))
        .start()
}

/** Aparece creciendo con un pequeño rebote. */
fun View.entrarConRebote(retraso: Long = 0) {
    alpha = 0f
    scaleX = 0.7f
    scaleY = 0.7f
    animate()
        .alpha(1f)
        .scaleX(1f)
        .scaleY(1f)
        .setStartDelay(retraso)
        .setDuration(520)
        .setInterpolator(OvershootInterpolator(1.6f))
        .start()
}

/** Sube y baja despacio sin parar. Hay que cancelarlo al destruir la vista. */
fun View.flotar(distanciaDp: Float = 6f): ObjectAnimator =
    ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, -dp(distanciaDp)).apply {
        duration = 2200
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        start()
    }
