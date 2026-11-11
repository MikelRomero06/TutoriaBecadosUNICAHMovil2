package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.ErrorValidacionDto
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

private val gson = Gson()

/** Ejecuta una llamada a la API y la traduce a [Resultado], quedándose con lo que devuelve [extraer]. */
suspend fun <T, R> llamarApi(
    bloque: suspend () -> Response<T>,
    extraer: (T) -> R,
): Resultado<R> {
    return try {
        val respuesta = bloque()
        val cuerpo = respuesta.body()
        when {
            respuesta.isSuccessful && cuerpo != null -> Resultado.Exito(extraer(cuerpo))
            respuesta.isSuccessful -> Resultado.ErrorApi(respuesta.code(), "El servidor respondió sin contenido.")
            respuesta.code() == 401 -> Resultado.NoAutorizado
            else -> {
                val error = leerError(respuesta.errorBody()?.string())
                if (respuesta.code() == 422) {
                    Resultado.ErrorValidacion(
                        error?.message ?: "Revisá los datos ingresados.",
                        error?.errors ?: emptyMap(),
                    )
                } else {
                    Resultado.ErrorApi(respuesta.code(), error?.message ?: "Error del servidor (${respuesta.code()}).")
                }
            }
        }
    } catch (_: IOException) {
        Resultado.ErrorRed("No se pudo conectar con el servidor.")
    } catch (_: Exception) {
        Resultado.ErrorApi(0, "Ocurrió un error inesperado.")
    }
}

/** Mensaje listo para mostrar de cualquier resultado con error. */
fun Resultado<*>.mensajeError(): String = when (this) {
    is Resultado.ErrorApi -> mensaje
    is Resultado.ErrorRed -> mensaje
    is Resultado.ErrorValidacion -> mensaje
    Resultado.NoAutorizado -> "Tu sesión venció. Iniciá sesión de nuevo."
    is Resultado.Exito -> ""
}

private fun leerError(cuerpo: String?): ErrorValidacionDto? {
    if (cuerpo.isNullOrBlank()) return null
    return try {
        gson.fromJson(cuerpo, ErrorValidacionDto::class.java)
    } catch (_: Exception) {
        null
    }
}
