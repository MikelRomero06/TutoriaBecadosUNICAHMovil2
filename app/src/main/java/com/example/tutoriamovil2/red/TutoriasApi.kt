package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.ListaMateriasRespuesta
import com.example.tutoriamovil2.red.dto.ListaTutoresRespuesta
import com.example.tutoriamovil2.red.dto.ListaTutoriasRespuesta
import com.example.tutoriamovil2.red.dto.TutorRespuesta
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TutoriasApi {

    /** Tutores aprobados. `carrera_id` acepta un id o `todas`; sin él, el servidor usa la carrera del usuario. */
    @GET("tutores")
    suspend fun tutores(
        @Query("carrera_id") carreraId: String? = null,
        @Query("materia_id") materiaId: Long? = null,
        @Query("modalidad") modalidad: String? = null,
        @Query("buscar") buscar: String? = null,
        @Query("page") pagina: Int? = null,
    ): Response<ListaTutoresRespuesta>

    @GET("tutores/{id}")
    suspend fun tutor(@Path("id") id: Long): Response<TutorRespuesta>

    @GET("materias")
    suspend fun materias(@Query("carrera_id") carreraId: Long? = null): Response<ListaMateriasRespuesta>

    /** `tipo` = `proximas` o `historial`. */
    @GET("mis-tutorias")
    suspend fun misTutorias(
        @Query("tipo") tipo: String,
        @Query("page") pagina: Int? = null,
    ): Response<ListaTutoriasRespuesta>
}
