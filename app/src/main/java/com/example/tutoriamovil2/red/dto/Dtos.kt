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

/** Datos de paginación de Laravel (`meta` en los listados paginados). */
data class MetaPaginacion(
    @SerializedName("current_page") val paginaActual: Int,
    @SerializedName("last_page") val ultimaPagina: Int,
    val total: Int,
)

data class MateriaDto(
    val id: Long,
    val nombre: String?,
    @SerializedName("carrera_id") val carreraId: Long?,
)

/** `GET materias?carrera_id=` responde `{ "data": [...] }`. */
data class ListaMateriasRespuesta(
    val data: List<MateriaDto>,
)

/** `GET tutores` responde paginado: `{ "data": [...], "meta": {...} }`. */
data class ListaTutoresRespuesta(
    val data: List<TutorDto>,
    val meta: MetaPaginacion?,
)

/** `GET tutores/{id}` responde `{ "data": {...} }`. */
data class TutorRespuesta(
    val data: TutorDto,
)

data class TutorDto(
    val id: Long,
    val name: String?,
    val carrera: CarreraDto?,
    @SerializedName("foto_url") val fotoUrl: String?,
    val materias: List<MateriaDto>?,
    @SerializedName("promedio_calificacion") val promedio: Double?,
    @SerializedName("total_calificaciones") val totalCalificaciones: Int?,
    val nuevo: Boolean?,
    val bio: String? = null,
    @SerializedName("total_cumplidas") val totalCumplidas: Int? = null,
)

/** `GET mis-tutorias?tipo=` responde paginado: `{ "data": [...], "meta": {...} }`. */
data class ListaTutoriasRespuesta(
    val data: List<TutoriaDto>,
    val meta: MetaPaginacion?,
)

data class TutoriaDto(
    val id: Long,
    val tema: String?,
    val descripcion: String?,
    val fecha: String?,
    @SerializedName("hora_inicio") val horaInicio: Int?,
    @SerializedName("hora_fin") val horaFin: Int?,
    val modalidad: String?,
    @SerializedName("lugar_o_enlace") val lugarOEnlace: String?,
    val estado: String?,
    @SerializedName("asistencia_confirmada") val asistenciaConfirmada: Boolean?,
    val materia: MateriaDto?,
    val tutor: TutorResumenDto?,
    val calificada: Boolean?,
)

data class TutorResumenDto(
    val id: Long,
    val name: String?,
    @SerializedName("foto_url") val fotoUrl: String?,
)

/** `GET notificaciones` y `GET citaciones` responden paginado: `{ "data": [...] }`. */
data class ListaNotificacionesRespuesta(
    val data: List<NotificacionDto>,
)

data class NotificacionDto(
    val id: String,
    val tipo: String?,
    val data: Map<String, Any?>?,
    val leida: Boolean,
    @SerializedName("created_at") val creadaEn: String?,
)

data class ListaCitacionesRespuesta(
    val data: List<CitacionDto>,
)

data class CitacionDto(
    val id: Long,
    val mensaje: String?,
    @SerializedName("fecha_cita") val fechaCita: String?,
    val enterado: Boolean,
)
