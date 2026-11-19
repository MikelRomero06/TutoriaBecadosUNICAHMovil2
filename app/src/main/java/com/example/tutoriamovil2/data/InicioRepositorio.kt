package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.NotificacionesApi
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.CitacionDto
import com.example.tutoriamovil2.red.dto.MensajeRespuesta
import com.example.tutoriamovil2.red.dto.NotificacionDto

/** Citaciones y notificaciones del usuario. */
class InicioRepositorio(private val api: NotificacionesApi) {

    suspend fun notificaciones(): Resultado<List<NotificacionDto>> =
        llamarApi({ api.notificaciones() }) { it.data }

    suspend fun citaciones(): Resultado<List<CitacionDto>> =
        llamarApi({ api.citaciones() }) { it.data }

    suspend fun marcarEnterado(id: Long): Resultado<MensajeRespuesta> =
        llamarApi({ api.marcarEnterado(id) }) { it }

    suspend fun leerTodas(): Resultado<MensajeRespuesta> =
        llamarApi({ api.leerTodas() }) { it }
}
