package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.Resultado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// Comprueba que cada respuesta de la API se convierta en el error correcto para las pantallas (tarea 5.10).
class LlamadaApiTest {

    private fun error(codigo: Int, cuerpo: String): Response<String> =
        Response.error(codigo, cuerpo.toResponseBody("application/json".toMediaType()))

    private fun llamar(respuesta: Response<String>) = runBlocking {
        llamarApi({ respuesta }) { it }
    }

    private fun lanzar(excepcion: Exception) = runBlocking {
        llamarApi<String, String>({ throw excepcion }) { it }
    }

    @Test
    fun `respuesta correcta entrega el dato`() {
        assertEquals(Resultado.Exito("hola"), llamar(Response.success("hola")))
    }

    @Test
    fun `respuesta correcta sin cuerpo es un error claro`() {
        val resultado = llamar(Response.success(204, null as String?))
        assertEquals(Resultado.ErrorApi(204, MensajesApi.SIN_CONTENIDO), resultado)
    }

    @Test
    fun `401 pide iniciar sesion`() {
        val resultado = llamar(error(401, """{"message":"Tu sesión venció."}"""))
        assertEquals(Resultado.NoAutorizado, resultado)
        assertEquals(MensajesApi.SESION_VENCIDA, resultado.mensajeError())
    }

    @Test
    fun `409 conserva el mensaje y se marca como conflicto`() {
        val resultado = llamar(error(409, """{"message":"Ese bloque ya fue reservado."}""")) as Resultado.ErrorApi
        assertTrue(resultado.esConflicto)
        assertEquals("Ese bloque ya fue reservado.", resultado.mensaje)
    }

    @Test
    fun `422 entrega los errores por campo`() {
        val cuerpo = """{"message":"Revisá","errors":{"tema":["El campo tema es obligatorio."]}}"""
        val resultado = llamar(error(422, cuerpo)) as Resultado.ErrorValidacion
        assertEquals("Revisá", resultado.mensaje)
        assertEquals(listOf("El campo tema es obligatorio."), resultado.errores["tema"])
    }

    @Test
    fun `sin mensaje del servidor usa el texto de cada codigo`() {
        mapOf(
            403 to MensajesApi.PROHIBIDO,
            404 to MensajesApi.NO_ENCONTRADO,
            409 to MensajesApi.CONFLICTO,
            429 to MensajesApi.DEMASIADAS_PETICIONES,
            500 to MensajesApi.SERVIDOR,
            503 to MensajesApi.SERVIDOR,
        ).forEach { (codigo, mensaje) ->
            assertEquals(Resultado.ErrorApi(codigo, mensaje), llamar(error(codigo, "{}")))
        }
    }

    @Test
    fun `un error que no es JSON no rompe la app`() {
        val resultado = llamar(error(500, "<html>Error</html>"))
        assertEquals(Resultado.ErrorApi(500, MensajesApi.SERVIDOR), resultado)
    }

    @Test
    fun `los fallos de red se traducen a mensajes claros`() {
        assertEquals(Resultado.ErrorRed(MensajesApi.TIEMPO_AGOTADO), lanzar(SocketTimeoutException()))
        assertEquals(Resultado.ErrorRed(MensajesApi.SIN_INTERNET), lanzar(UnknownHostException()))
        assertEquals(Resultado.ErrorRed(MensajesApi.SIN_CONEXION), lanzar(IOException()))
    }

    @Test(expected = CancellationException::class)
    fun `salir de la pantalla no se confunde con un error`() {
        lanzar(CancellationException("pantalla cerrada"))
    }
}
