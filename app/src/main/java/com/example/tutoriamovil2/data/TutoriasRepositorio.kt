package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.TutoriasApi
import com.example.tutoriamovil2.red.dto.CalificacionPeticion
import com.example.tutoriamovil2.red.dto.CancelacionPeticion
import com.example.tutoriamovil2.red.dto.InasistenciaPeticion
import com.example.tutoriamovil2.red.dto.BloqueDto
import com.example.tutoriamovil2.red.dto.ListaTutoresRespuesta
import com.example.tutoriamovil2.red.dto.ListaTutoriasRespuesta
import com.example.tutoriamovil2.red.dto.MateriaDto
import com.example.tutoriamovil2.red.dto.PostulacionPeticion
import com.example.tutoriamovil2.red.dto.SolicitudPeticion
import com.example.tutoriamovil2.red.dto.TutorDto
import com.example.tutoriamovil2.red.dto.TutoriaDto

/** Filtros de la búsqueda de tutores. `carreraId` nulo = todas las carreras. */
data class FiltroTutores(
    val carreraId: Long? = null,
    val materiaId: Long? = null,
    val modalidad: String? = null,
    val buscar: String = "",
)

/** Tutores, materias y tutorías (del estudiante y de la agenda del tutor). */
class TutoriasRepositorio(private val api: TutoriasApi) {

    /** Con `deMiCarrera` y sin carrera en el filtro, el servidor usa la carrera del usuario. */
    suspend fun obtenerTutores(
        filtro: FiltroTutores,
        pagina: Int = 1,
        deMiCarrera: Boolean = false,
    ): Resultado<ListaTutoresRespuesta> =
        llamarApi({
            api.tutores(
                carreraId = filtro.carreraId?.toString() ?: if (deMiCarrera) null else "todas",
                materiaId = filtro.materiaId,
                modalidad = filtro.modalidad,
                buscar = filtro.buscar.takeIf { it.isNotBlank() },
                pagina = pagina,
            )
        }) { it }

    suspend fun obtenerTutor(id: Long): Resultado<TutorDto> =
        llamarApi({ api.tutor(id) }) { it.data }

    suspend fun obtenerMaterias(carreraId: Long?): Resultado<List<MateriaDto>> =
        llamarApi({ api.materias(carreraId) }) { it.data }

    suspend fun misTutorias(tipo: String, pagina: Int = 1): Resultado<ListaTutoriasRespuesta> =
        llamarApi({ api.misTutorias(tipo, pagina) }) { it }

    suspend fun bloques(tutorId: Long, fecha: String): Resultado<List<BloqueDto>> =
        llamarApi({ api.bloques(tutorId, fecha) }) { it.data }

    suspend fun miAgenda(tipo: String, pagina: Int = 1): Resultado<ListaTutoriasRespuesta> =
        llamarApi({ api.miAgenda(tipo, pagina) }) { it }

    suspend fun solicitarTutoria(peticion: SolicitudPeticion): Resultado<TutoriaDto> =
        llamarApi({ api.solicitar(peticion) }) { it.data }

    suspend fun cancelarTutoria(id: Long, motivo: String): Resultado<Unit> =
        llamarApi({ api.cancelar(id, CancelacionPeticion(motivo)) }) { }

    suspend fun reportarInasistencia(id: Long, motivo: String): Resultado<Unit> =
        llamarApi({ api.reportarInasistencia(id, InasistenciaPeticion(motivo)) }) { }

    suspend fun calificarTutoria(id: Long, puntaje: Int, comentario: String?): Resultado<Unit> =
        llamarApi({ api.calificar(id, CalificacionPeticion(puntaje, comentario)) }) { }

    suspend fun postularse(materiaIds: List<Long>, bio: String?): Resultado<Unit> =
        llamarApi({ api.postularse(PostulacionPeticion(materiaIds, bio)) }) { }
}
