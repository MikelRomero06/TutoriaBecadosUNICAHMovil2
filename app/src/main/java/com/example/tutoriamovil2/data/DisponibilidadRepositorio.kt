package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.DisponibilidadApi
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.DisponibilidadDto
import com.example.tutoriamovil2.red.dto.GuardarDisponibilidadPeticion

// Lee y guarda los horarios del tutor. Las dos llamadas devuelven la lista tal como quedó en el servidor.
class DisponibilidadRepositorio(private val api: DisponibilidadApi) {

    suspend fun obtener(): Resultado<List<DisponibilidadDto>> =
        llamarApi({ api.miDisponibilidad() }) { it.data }

    suspend fun guardar(bloques: List<DisponibilidadDto>): Resultado<List<DisponibilidadDto>> =
        llamarApi({ api.guardar(GuardarDisponibilidadPeticion(bloques)) }) { it.data }
}
