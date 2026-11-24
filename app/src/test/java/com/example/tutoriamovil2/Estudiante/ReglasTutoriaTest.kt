package com.example.tutoriamovil2.Estudiante

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Reglas que la app muestra al usuario y deben coincidir con las del servidor.
class ReglasTutoriaTest {

    @Test
    fun `solo una tutoria confirmada ofrece cancelar`() {
        assertTrue(puedeCancelar("CONFIRMADA"))
    }

    // El API responde 409 a cualquier estado que no sea CONFIRMADA.
    @Test
    fun `una tutoria cerrada no ofrece cancelar`() {
        listOf(
            "COMPLETADA", "SIN_CONFIRMAR", "NO_ASISTIO_TUTOR", "NO_ASISTIO_ESTUDIANTE",
            "CANCELADA_TUTOR", "CANCELADA_ESTUDIANTE", null,
        ).forEach { estado ->
            assertFalse("No debería poder cancelarse en estado $estado", puedeCancelar(estado))
        }
    }

    @Test
    fun `formatea horas y rangos`() {
        assertEquals("07:00", horaTexto(7))
        assertEquals("14:00 – 15:00", rangoHoras(14, 15))
        assertEquals("09:00 – 10:00", horario(9, null))
        assertEquals("", horario(null, null))
    }

    @Test
    fun `la fecha larga queda en espanol`() {
        assertEquals("Martes 24 nov", fechaLarga("2026-11-24"))
    }

    @Test
    fun `una fecha invalida no rompe la tarjeta`() {
        assertEquals("no-es-fecha", fechaCorta("no-es-fecha"))
        assertEquals("", fechaCorta(null))
    }

    @Test
    fun `dos bloques se distinguen por fecha y hora`() {
        val bloque = BloqueUi("2026-11-24", 14, 15, "VIRTUAL", null)
        assertEquals("2026-11-24#14", bloque.clave)
        assertFalse(bloque.clave == bloque.copy(horaInicio = 15).clave)
    }
}
