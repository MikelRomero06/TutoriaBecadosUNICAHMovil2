package com.example.tutoriamovil2.red.dto

import com.google.gson.annotations.SerializedName

// Bloque de una fecha con su disponibilidad real.
data class BloqueDto(
    @SerializedName("hora_inicio") val horaInicio: Int,
    @SerializedName("hora_fin") val horaFin: Int,
    val modalidad: String?,
    @SerializedName("lugar_o_enlace") val lugarOEnlace: String?,
    val estado: String,
    val disponible: Boolean,
    val motivo: String?,
)

// Lista de bloques de la fecha consultada.
data class ListaBloquesRespuesta(val data: List<BloqueDto>)
