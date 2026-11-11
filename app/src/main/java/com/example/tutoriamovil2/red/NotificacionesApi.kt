package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.ListaCitacionesRespuesta
import com.example.tutoriamovil2.red.dto.ListaNotificacionesRespuesta
import com.example.tutoriamovil2.red.dto.MensajeRespuesta
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificacionesApi {

    @GET("notificaciones")
    suspend fun notificaciones(): Response<ListaNotificacionesRespuesta>

    @POST("notificaciones/leer-todas")
    suspend fun leerTodas(): Response<MensajeRespuesta>

    @GET("citaciones")
    suspend fun citaciones(): Response<ListaCitacionesRespuesta>

    @POST("citaciones/{id}/enterado")
    suspend fun marcarEnterado(@Path("id") id: Long): Response<MensajeRespuesta>
}
