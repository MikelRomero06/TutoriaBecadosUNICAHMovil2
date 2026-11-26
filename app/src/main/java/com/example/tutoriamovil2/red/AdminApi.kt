package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.AdminResumenRespuesta
import com.example.tutoriamovil2.red.dto.ListaAdminTutoriasRespuesta
import com.example.tutoriamovil2.red.dto.ListaCambiosFotoRespuesta
import com.example.tutoriamovil2.red.dto.ListaPostulacionesRespuesta
import com.example.tutoriamovil2.red.dto.MensajeRespuesta
import com.example.tutoriamovil2.red.dto.RechazoPeticion
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Endpoints del administrador (Bienestar).
 * OJO: rutas supuestas; confirmarlas con el servidor antes de integrar.
 */
interface AdminApi {

    @GET("admin/resumen")
    suspend fun resumen(): Response<AdminResumenRespuesta>

    @GET("admin/tutorias-hoy")
    suspend fun tutoriasHoy(): Response<ListaAdminTutoriasRespuesta>

    @GET("admin/postulaciones")
    suspend fun postulaciones(): Response<ListaPostulacionesRespuesta>

    @POST("admin/postulaciones/{id}/aprobar")
    suspend fun aprobarPostulacion(@Path("id") id: Long): Response<MensajeRespuesta>

    /** El motivo es obligatorio al rechazar una postulación. */
    @POST("admin/postulaciones/{id}/rechazar")
    suspend fun rechazarPostulacion(@Path("id") id: Long, @Body peticion: RechazoPeticion): Response<MensajeRespuesta>

    @GET("admin/fotos")
    suspend fun cambiosFoto(): Response<ListaCambiosFotoRespuesta>

    @POST("admin/fotos/{id}/aprobar")
    suspend fun aprobarFoto(@Path("id") id: Long): Response<MensajeRespuesta>

    @POST("admin/fotos/{id}/rechazar")
    suspend fun rechazarFoto(@Path("id") id: Long, @Body peticion: RechazoPeticion): Response<MensajeRespuesta>
}
