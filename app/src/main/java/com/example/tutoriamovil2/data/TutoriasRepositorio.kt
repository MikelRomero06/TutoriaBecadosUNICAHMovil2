package com.example.tutoriamovil2.data

import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.TutoriasApi
import com.example.tutoriamovil2.red.dto.ListaTutoresRespuesta
import com.example.tutoriamovil2.red.dto.ListaTutoriasRespuesta
import com.example.tutoriamovil2.red.dto.MateriaDto
import com.example.tutoriamovil2.red.dto.TutorDto

/** Filtros de la búsqueda de tutores. `carreraId` nulo = todas las carreras. */
data class FiltroTutores(
    val carreraId: Long? = null,
    val materiaId: Long? = null,
    val modalidad: String? = null,
    val buscar: String = "",
)

/** Tutores, materias y tutorías del estudiante. */
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
}
