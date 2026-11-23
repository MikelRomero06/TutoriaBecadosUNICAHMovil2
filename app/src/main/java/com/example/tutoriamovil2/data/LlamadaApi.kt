package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.ErrorValidacionDto
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private val gson = Gson()

/** Textos por defecto cuando el servidor no manda `message`. Un solo lugar para todos los errores de la API. */
object MensajesApi {
    const val SIN_CONTENIDO = "El servidor respondió sin contenido."
    const val SIN_CONEXION = "No se pudo conectar con el servidor."
    const val SIN_INTERNET = "No hay conexión a internet."
    const val TIEMPO_AGOTADO = "La conexión tardó demasiado. Intentá de nuevo."
    const val INESPERADO = "Ocurrió un error inesperado."
    const val VALIDACION = "Revisá los datos ingresados."
    const val SESION_VENCIDA = "Tu sesión venció. Iniciá sesión de nuevo."
    const val PROHIBIDO = "No tenés permiso para hacer esto."
    const val NO_ENCONTRADO = "No encontramos lo que buscabas."
    const val CONFLICTO = "Ese horario ya no está disponible. Elegí otro bloque."
    const val DEMASIADAS_PETICIONES = "Demasiados intentos. Esperá un momento y volvé a probar."
    const val SERVIDOR = "El servidor tuvo un problema. Intentá más tarde."

    /** Mensaje por código HTTP cuando la respuesta de error no trae uno propio. */
    fun porCodigo(codigo: Int): String = when (codigo) {
        403 -> PROHIBIDO
        404 -> NO_ENCONTRADO
        409 -> CONFLICTO
        422 -> VALIDACION
        429 -> DEMASIADAS_PETICIONES
        in 500..599 -> SERVIDOR
        else -> "Error del servidor ($codigo)."
    }
}

/**
 * Ejecuta una llamada a la API y la traduce a [Resultado], quedándose con lo que devuelve [extraer].
 *
 * Mapeo uniforme:
 * - 2xx → [Resultado.Exito]
 * - 401 → [Resultado.NoAutorizado]
 * - 422 → [Resultado.ErrorValidacion] (mensaje + errores por campo)
 * - 409 → [Resultado.ErrorApi] con `esConflicto` (p. ej. bloque ya reservado)
 * - 403, 404, 429, 5xx y demás → [Resultado.ErrorApi] con el mensaje del servidor o el de [MensajesApi]
 * - sin red / timeout → [Resultado.ErrorRed]
 */
suspend fun <T, R> llamarApi(
    bloque: suspend () -> Response<T>,
    extraer: (T) -> R,
): Resultado<R> {
    return try {
        val respuesta = bloque()
        val cuerpo = respuesta.body()
        when {
            respuesta.isSuccessful && cuerpo != null -> Resultado.Exito(extraer(cuerpo))
            respuesta.isSuccessful -> Resultado.ErrorApi(respuesta.code(), MensajesApi.SIN_CONTENIDO)
            respuesta.code() == 401 -> Resultado.NoAutorizado
            else -> {
                val codigo = respuesta.code()
                val error = leerError(respuesta.errorBody()?.string())
                val mensaje = error?.message?.takeIf { it.isNotBlank() } ?: MensajesApi.porCodigo(codigo)
                if (codigo == 422) Resultado.ErrorValidacion(mensaje, error?.errors ?: emptyMap())
                else Resultado.ErrorApi(codigo, mensaje)
            }
        }
    } catch (e: CancellationException) {
        throw e // no tragarse la cancelación de la corrutina (p. ej. al salir de la pantalla)
    } catch (_: SocketTimeoutException) {
        Resultado.ErrorRed(MensajesApi.TIEMPO_AGOTADO)
    } catch (_: UnknownHostException) {
        Resultado.ErrorRed(MensajesApi.SIN_INTERNET)
    } catch (_: IOException) {
        Resultado.ErrorRed(MensajesApi.SIN_CONEXION)
    } catch (_: Exception) {
        Resultado.ErrorApi(0, MensajesApi.INESPERADO)
    }
}

/** Mensaje listo para mostrar de cualquier resultado con error. */
fun Resultado<*>.mensajeError(): String = when (this) {
    is Resultado.ErrorApi -> mensaje
    is Resultado.ErrorRed -> mensaje
    is Resultado.ErrorValidacion -> mensaje
    Resultado.NoAutorizado -> MensajesApi.SESION_VENCIDA
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
