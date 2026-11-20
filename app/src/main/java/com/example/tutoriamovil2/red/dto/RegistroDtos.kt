package com.example.tutoriamovil2.red.dto

import com.google.gson.annotations.SerializedName

data class EnviarCodigoPeticion(
    val correo: String,
)

data class EnviarCodigoRespuesta(
    val message: String?,
    @SerializedName("expira_en_minutos") val expiraEnMinutos: Int?,
    @SerializedName("reenviar_en_segundos") val reenviarEnSegundos: Int?,
)

data class VerificarCodigoPeticion(
    val correo: String,
    val codigo: String,
)

data class VerificarCodigoRespuesta(
    val message: String?,
    @SerializedName("registro_token") val registroToken: String,
    @SerializedName("expira_en_minutos") val expiraEnMinutos: Int?,
)

data class CompletarRegistroPeticion(
    @SerializedName("registro_token") val registroToken: String,
    val name: String,
    @SerializedName("carrera_id") val carreraId: Long,
    val telefono: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String,
    @SerializedName("device_name") val deviceName: String,
)

data class CompletarRegistroRespuesta(
    val message: String?,
    val token: String,
    val usuario: UsuarioDto,
)

/** `GET auth/registro/carreras` responde `{ "data": [ {...} ] }` (colección de API Resources). */
data class CarrerasRespuesta(
    val data: List<CarreraDto>,
)

data class CarreraDto(
    val id: Long,
    val nombre: String,
    val facultad: String?,
)

data class FotoRespuesta(
    val message: String?,
    @SerializedName("foto_url") val fotoUrl: String?,
)
