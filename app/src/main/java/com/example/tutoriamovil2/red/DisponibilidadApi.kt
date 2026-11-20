package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.GuardarDisponibilidadPeticion
import com.example.tutoriamovil2.red.dto.ListaDisponibilidadRespuesta
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

// Horarios del tutor que inició sesión. Solo funciona para tutores aprobados; a los demás el servidor les responde 403.
interface DisponibilidadApi {

    @GET("mi-disponibilidad")
    suspend fun miDisponibilidad(): Response<ListaDisponibilidadRespuesta>

    // Manda todos los horarios juntos; el servidor borra los anteriores y guarda estos.
    @PUT("mi-disponibilidad")
    suspend fun guardar(@Body peticion: GuardarDisponibilidadPeticion): Response<ListaDisponibilidadRespuesta>
}
