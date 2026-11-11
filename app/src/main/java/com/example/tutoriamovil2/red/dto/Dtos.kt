package com.example.tutoriamovil2.red.dto

import com.google.gson.annotations.SerializedName

data class LoginPeticion(
    val email: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String,
)

data class LoginRespuesta(
    val message: String?,
    val token: String,
    val usuario: UsuarioDto,
)

/** `GET auth/yo` responde `{ "usuario": {...} }`. */
data class UsuarioRespuesta(
    val usuario: UsuarioDto,
)

data class UsuarioDto(
    val id: Long,
    val name: String,
    val email: String,
    val rol: String,
    @SerializedName("carrera_id") val carreraId: Long?,
    val carrera: CarreraDto?,
    val telefono: String?,
    @SerializedName("foto_url") val fotoUrl: String?,
    val activo: Boolean,
    @SerializedName("es_tutor_aprobado") val esTutorAprobado: Boolean,
)

data class MensajeRespuesta(
    val message: String?,
)

/** Formato de error de validación de la API: `{ "message", "errors": { campo: [...] } }`. */
data class ErrorValidacionDto(
    val message: String?,
    val errors: Map<String, List<String>>?,
)
