package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.AdminApi
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.AdminResumenDto
import com.example.tutoriamovil2.red.dto.CambioFotoDto
import com.example.tutoriamovil2.red.dto.PostulacionDto
import com.example.tutoriamovil2.red.dto.RechazoPeticion
import com.example.tutoriamovil2.red.dto.TutoriaDto

/** Inicio y aprobaciones del administrador. */
class AdminRepositorio(private val api: AdminApi) {

    suspend fun resumen(): Resultado<AdminResumenDto> =
        llamarApi({ api.resumen() }) { it.data }

    suspend fun tutoriasHoy(): Resultado<List<TutoriaDto>> =
        llamarApi({ api.tutoriasHoy() }) { it.data }

    suspend fun postulaciones(): Resultado<List<PostulacionDto>> =
        llamarApi({ api.postulaciones() }) { it.data }

    suspend fun aprobarPostulacion(id: Long): Resultado<Unit> =
        llamarApi({ api.aprobarPostulacion(id) }) { }

    suspend fun rechazarPostulacion(id: Long, motivo: String): Resultado<Unit> =
        llamarApi({ api.rechazarPostulacion(id, RechazoPeticion(motivo)) }) { }

    suspend fun cambiosFoto(): Resultado<List<CambioFotoDto>> =
        llamarApi({ api.cambiosFoto() }) { it.data }

    suspend fun aprobarFoto(id: Long): Resultado<Unit> =
        llamarApi({ api.aprobarFoto(id) }) { }

    suspend fun rechazarFoto(id: Long, motivo: String?): Resultado<Unit> =
        llamarApi({ api.rechazarFoto(id, RechazoPeticion(motivo)) }) { }
}
