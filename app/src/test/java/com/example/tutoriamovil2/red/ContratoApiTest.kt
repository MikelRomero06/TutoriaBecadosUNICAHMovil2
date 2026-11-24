package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.CancelacionPeticion
import com.example.tutoriamovil2.red.dto.ListaBloquesRespuesta
import com.example.tutoriamovil2.red.dto.ListaTutoriasRespuesta
import com.example.tutoriamovil2.red.dto.SolicitudPeticion
import com.example.tutoriamovil2.red.dto.TutoriaRespuesta
import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Los JSON siguen el formato de TutoriaResource y BloqueResource de Laravel.
// Si el API cambia un nombre de campo, estas pruebas avisan antes de llegar a la pantalla.
class ContratoApiTest {

    private val gson = Gson()

    private val tutoriaJson = """
        {"id":7,"tema":"Integrales","descripcion":"Repaso","fecha":"2026-11-24","hora_inicio":14,"hora_fin":15,
         "modalidad":"VIRTUAL","lugar_o_enlace":"https://example.com/sala","estado":"CONFIRMADA",
         "asistencia_confirmada":false,"materia":{"id":3,"nombre":"Cálculo"},
         "tutor":{"id":2,"name":"Tutor Prueba","foto_url":null,"telefono":"9999-9999"},
         "estudiante":{"id":5,"name":"Estudiante Prueba","foto_url":null,"telefono":"8888-8888"},
         "calificada":false}
    """.trimIndent()

    @Test
    fun `lee una tutoria completa`() {
        val tutoria = gson.fromJson("""{"data":$tutoriaJson}""", TutoriaRespuesta::class.java).data
        assertEquals(7L, tutoria.id)
        assertEquals(14, tutoria.horaInicio)
        assertEquals(15, tutoria.horaFin)
        assertEquals("https://example.com/sala", tutoria.lugarOEnlace)
        assertEquals(false, tutoria.asistenciaConfirmada)
        assertEquals("Cálculo", tutoria.materia?.nombre)
        assertEquals("Tutor Prueba", tutoria.tutor?.name)
        assertEquals("Estudiante Prueba", tutoria.estudiante?.name)
    }

    @Test
    fun `lee la agenda paginada aunque no venga el tutor`() {
        val sinTutor = tutoriaJson.replace(""""tutor":{"id":2,"name":"Tutor Prueba","foto_url":null,"telefono":"9999-9999"},""", "")
        val json = """{"data":[$sinTutor],"meta":{"current_page":1,"last_page":3,"total":31}}"""
        val lista = gson.fromJson(json, ListaTutoriasRespuesta::class.java)
        assertEquals(1, lista.data.size)
        assertNull(lista.data.first().tutor)
        assertEquals(3, lista.meta?.ultimaPagina)
        assertEquals(31, lista.meta?.total)
    }

    @Test
    fun `lee bloques libres y ocupados`() {
        val json = """{"data":[
            {"hora_inicio":14,"hora_fin":15,"modalidad":"PRESENCIAL","lugar_o_enlace":"Aula 3","estado":"libre","disponible":true,"motivo":null},
            {"hora_inicio":15,"hora_fin":16,"modalidad":"VIRTUAL","lugar_o_enlace":null,"estado":"ocupado","disponible":false,"motivo":"bloque_reservado"}
        ]}"""
        val bloques = gson.fromJson(json, ListaBloquesRespuesta::class.java).data
        assertTrue(bloques[0].disponible)
        assertEquals("Aula 3", bloques[0].lugarOEnlace)
        assertFalse(bloques[1].disponible)
        assertEquals("bloque_reservado", bloques[1].motivo)
    }

    @Test
    fun `la reserva envia solo los campos que acepta el API`() {
        val peticion = SolicitudPeticion(2, "2026-11-24", 14, 3, "Integrales", "Repaso para el parcial")
        val json = JsonParser.parseString(gson.toJson(peticion)).asJsonObject
        assertEquals(setOf("tutor_id", "fecha", "hora_inicio", "materia_id", "tema", "descripcion"), json.keySet())
        assertEquals(14, json["hora_inicio"].asInt)
    }

    @Test
    fun `la cancelacion envia el motivo`() {
        val json = JsonParser.parseString(gson.toJson(CancelacionPeticion("Me enfermé"))).asJsonObject
        assertEquals("Me enfermé", json["motivo"].asString)
    }
}
