package com.example.tutoriamovil2.red.dto

import com.google.gson.annotations.SerializedName

/** `GET admin/resumen`: contadores del inicio del administrador. */
data class AdminResumenDto(
    @SerializedName("aprobaciones_pendientes") val aprobacionesPendientes: Int = 0,
    @SerializedName("no_realizadas_sin_revisar") val noRealizadasSinRevisar: Int = 0,
    @SerializedName("eventos_sin_revisar") val eventosSinRevisar: Int = 0,
    @SerializedName("tutorias_hoy") val tutoriasHoy: Int = 0,
)

data class AdminResumenRespuesta(val data: AdminResumenDto)

/** `GET admin/tutorias-hoy`: tutorías del día con tutor, estudiante y materia (mismo formato que `TutoriaDto`). */
data class ListaAdminTutoriasRespuesta(val data: List<TutoriaDto>)

/** Postulación a tutor pendiente de revisión (`GET admin/postulaciones`). */
data class PostulacionDto(
    val id: Long,
    val usuario: EstudianteResumenDto?,
    val materias: List<MateriaDto>?,
    val bio: String? = null,
)

data class ListaPostulacionesRespuesta(val data: List<PostulacionDto>)

/** Cambio de foto de perfil pendiente de revisión (`GET admin/fotos`). */
data class CambioFotoDto(
    val id: Long,
    val usuario: EstudianteResumenDto?,
    @SerializedName("foto_anterior_url") val fotoAnteriorUrl: String?,
    @SerializedName("foto_nueva_url") val fotoNuevaUrl: String?,
)

data class ListaCambiosFotoRespuesta(val data: List<CambioFotoDto>)

/** Rechazo de una postulación (motivo obligatorio) o de una foto (motivo opcional). */
data class RechazoPeticion(val motivo: String?)
