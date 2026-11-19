package com.example.tutoriamovil2.red

import okhttp3.Interceptor
import okhttp3.Response

/** Ante un 401 borra el token y avisa para volver al login. */
class SesionExpiradaInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val respuesta = chain.proceed(chain.request())

        if (respuesta.code == 401 && SesionStore.hayToken()) {
            SesionStore.borrar()
            EventosSesion.caducar()
        }

        return respuesta
    }
}
