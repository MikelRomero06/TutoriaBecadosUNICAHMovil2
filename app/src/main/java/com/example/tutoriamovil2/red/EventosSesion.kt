package com.example.tutoriamovil2.red

/**
 * Punto único para avisar que la sesión caducó (un 401 del servidor). La Activity
 * que corresponda registra su reacción (volver al login) en [alCaducar].
 */
object EventosSesion {
    @Volatile
    var alCaducar: (() -> Unit)? = null

    fun caducar() {
        alCaducar?.invoke()
    }
}
