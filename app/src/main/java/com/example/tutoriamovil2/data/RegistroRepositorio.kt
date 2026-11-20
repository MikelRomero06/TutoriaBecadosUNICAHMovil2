package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.RegistroApi
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.CarrerasRespuesta
import com.example.tutoriamovil2.red.dto.CompletarRegistroPeticion
import com.example.tutoriamovil2.red.dto.CompletarRegistroRespuesta
import com.example.tutoriamovil2.red.dto.EnviarCodigoPeticion
import com.example.tutoriamovil2.red.dto.EnviarCodigoRespuesta
import com.example.tutoriamovil2.red.dto.ErrorValidacionDto
import com.example.tutoriamovil2.red.dto.FotoRespuesta
import com.example.tutoriamovil2.red.dto.VerificarCodigoPeticion
import com.example.tutoriamovil2.red.dto.VerificarCodigoRespuesta
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.IOException

/** Envuelve el registro por código y traduce las respuestas a [Resultado]. */
class RegistroRepositorio(private val api: RegistroApi) {

    private val gson = Gson()

    suspend fun enviarCodigo(correo: String): Resultado<EnviarCodigoRespuesta> =
        ejecutar { api.enviarCodigo(EnviarCodigoPeticion(correo)) }

    suspend fun verificarCodigo(correo: String, codigo: String): Resultado<VerificarCodigoRespuesta> =
        ejecutar { api.verificarCodigo(VerificarCodigoPeticion(correo, codigo)) }

    suspend fun completar(peticion: CompletarRegistroPeticion): Resultado<CompletarRegistroRespuesta> =
        ejecutar { api.completar(peticion) }

    suspend fun carreras(): Resultado<CarrerasRespuesta> =
        ejecutar { api.carreras() }

    suspend fun subirFoto(bytes: ByteArray, nombre: String, mime: String): Resultado<FotoRespuesta> {
        val cuerpo = bytes.toRequestBody(mime.toMediaTypeOrNull())
        val parte = MultipartBody.Part.createFormData("foto", nombre, cuerpo)
        return ejecutar { api.subirFoto(parte) }
    }

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
