package com.example.tutoriamovil2.Estudiante

import com.example.tutoriamovil2.red.dto.CitacionDto
import com.example.tutoriamovil2.red.dto.NotificacionDto

/** Elemento de la lista de avisos: una citación de Bienestar o una notificación. */
data class AvisoInicio(
    val clave: String,
    val titulo: String,
    val mensaje: String,
    val esCitacion: Boolean,
    val citacionId: Long? = null,
    val pendiente: Boolean = false,
)

fun avisoDesdeCitacion(c: CitacionDto) = AvisoInicio(
    clave = "c${c.id}",
    titulo = "Citación de Bienestar",
    mensaje = listOfNotNull(c.mensaje?.ifBlank { null }, c.fechaCita?.let { "Fecha: ${fechaCorta(it.take(10))}" })
        .joinToString("\n"),
    esCitacion = true,
    citacionId = c.id,
    pendiente = !c.enterado,
)

fun avisoDesdeNotificacion(n: NotificacionDto): AvisoInicio {
    val datos = n.data.orEmpty()
    val titulo = (datos["titulo"] as? String)
        ?: n.tipo?.let(::separarPalabras)
        ?: "Notificación"
    return AvisoInicio(
        clave = "n${n.id}",
        titulo = titulo,
        mensaje = (datos["mensaje"] as? String).orEmpty(),
        esCitacion = false,
        pendiente = !n.leida,
    )
}

/** "TutoriaReservada" -> "Tutoria reservada". */
private fun separarPalabras(tipo: String): String =
    tipo.substringAfterLast('\\')
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .lowercase()
        .replaceFirstChar { it.uppercase() }
