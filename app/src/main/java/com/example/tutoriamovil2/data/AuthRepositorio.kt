package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.AuthApi
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.ErrorValidacionDto
import com.example.tutoriamovil2.red.dto.LoginPeticion
import com.example.tutoriamovil2.red.dto.LoginRespuesta
import com.example.tutoriamovil2.red.dto.MensajeRespuesta
import com.example.tutoriamovil2.red.dto.UsuarioRespuesta
import com.google.gson.Gson
import retrofit2.Response
import java.io.IOException

/** Envuelve las llamadas de autenticación y traduce las respuestas a [Resultado]. */
class AuthRepositorio(private val api: AuthApi) {

    private val gson = Gson()

    suspend fun login(email: String, password: String, dispositivo: String): Resultado<LoginRespuesta> =
        ejecutar { api.login(LoginPeticion(email, password, dispositivo)) }

    suspend fun yo(): Resultado<UsuarioRespuesta> =
        ejecutar { api.yo() }

    suspend fun logout(): Resultado<MensajeRespuesta> =
        ejecutar { api.logout() }

    private suspend fun <T> ejecutar(bloque: suspend () -> Response<T>): Resultado<T> {
        return try {
            val respuesta = bloque()
            if (respuesta.isSuccessful) {
                val cuerpo = respuesta.body()
                if (cuerpo != null) {
                    Resultado.Exito(cuerpo)
                } else {
                    Resultado.ErrorApi(respuesta.code(), "El servidor respondió sin contenido.")
                }
            } else {
                mapearError(respuesta.code(), respuesta.errorBody()?.string())
            }
        } catch (_: IOException) {
            Resultado.ErrorRed("No se pudo conectar con el servidor.")
        } catch (_: Exception) {
            Resultado.ErrorApi(0, "Ocurrió un error inesperado.")
        }
    }

    private fun mapearError(codigo: Int, cuerpo: String?): Resultado<Nothing> {
        return when (codigo) {
            401 -> Resultado.NoAutorizado
            422 -> {
                val error = parsear(cuerpo)
                Resultado.ErrorValidacion(
                    mensaje = error?.message ?: "Revisá los datos ingresados.",
                    errores = error?.errors ?: emptyMap(),
                )
            }
            else -> Resultado.ErrorApi(
                codigo,
                parsear(cuerpo)?.message ?: "Error del servidor ($codigo).",
            )
        }
    }

    private fun parsear(cuerpo: String?): ErrorValidacionDto? {
        if (cuerpo.isNullOrBlank()) return null
        return try {
            gson.fromJson(cuerpo, ErrorValidacionDto::class.java)
        } catch (_: Exception) {
            null
        }
    }
}
