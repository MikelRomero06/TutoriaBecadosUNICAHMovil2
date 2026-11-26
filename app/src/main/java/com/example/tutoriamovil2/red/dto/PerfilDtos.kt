package com.example.tutoriamovil2.red.dto

import com.google.gson.annotations.SerializedName

data class SolicitudFotoDto(
    val id: Long,
    @SerializedName("foto_nueva_url") val fotoNuevaUrl: String?,
    val estado: String?,
    @SerializedName("motivo_rechazo") val motivoRechazo: String?,
)

// 200 trae foto_url (se aplicó); 202 trae la solicitud pendiente.
data class SubirFotoRespuesta(
    val message: String?,
    @SerializedName("foto_url") val fotoUrl: String?,
    val solicitud: SolicitudFotoDto?,
)

data class SolicitudFotoRespuesta(
    val solicitud: SolicitudFotoDto?,
)
