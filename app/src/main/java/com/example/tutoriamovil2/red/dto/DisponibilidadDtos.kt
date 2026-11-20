package com.example.tutoriamovil2.red.dto

import com.google.gson.annotations.SerializedName

// Un horario que el tutor publica: un día de la semana y una hora de inicio.
// Cada bloque dura una hora (si hora_inicio es 9, el bloque es de 9:00 a 10:00).
// dia_semana va de 1 (lunes) a 6 (sábado); no hay tutorías los domingos.
data class DisponibilidadDto(
    @SerializedName("dia_semana") val diaSemana: Int,
    @SerializedName("hora_inicio") val horaInicio: Int,
    val modalidad: String, // "PRESENCIAL" o "VIRTUAL"
    // Aula o enlace de la reunión. Si va vacío, el servidor usa el que el tutor tiene por defecto en su perfil.
    @SerializedName("lugar_o_enlace") val lugarOEnlace: String?,
)

data class ListaDisponibilidadRespuesta(
    val data: List<DisponibilidadDto>,
)

// El servidor no agrega ni borra horarios sueltos: siempre recibe la lista completa y la reemplaza.
data class GuardarDisponibilidadPeticion(
    val bloques: List<DisponibilidadDto>,
)
