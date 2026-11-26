package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.CalificacionPeticion
import com.example.tutoriamovil2.red.dto.CancelacionPeticion
import com.example.tutoriamovil2.red.dto.InasistenciaPeticion
import com.example.tutoriamovil2.red.dto.ListaBloquesRespuesta
import com.example.tutoriamovil2.red.dto.ListaMateriasRespuesta
import com.example.tutoriamovil2.red.dto.ListaTutoresRespuesta
import com.example.tutoriamovil2.red.dto.MensajeRespuesta
import com.example.tutoriamovil2.red.dto.PostulacionPeticion
import com.example.tutoriamovil2.red.dto.ListaTutoriasRespuesta
import com.example.tutoriamovil2.red.dto.SolicitudPeticion
import com.example.tutoriamovil2.red.dto.TutorRespuesta
import com.example.tutoriamovil2.red.dto.TutoriaRespuesta
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    @GET("mi-agenda")
    suspend fun miAgenda(@Query("tipo") tipo: String, @Query("page") pagina: Int): Response<ListaTutoriasRespuesta>

    @GET("tutores/{id}/bloques")
    suspend fun bloques(@Path("id") tutorId: Long, @Query("fecha") fecha: String): Response<ListaBloquesRespuesta>

    @POST("tutorias")
    suspend fun solicitar(@Body peticion: SolicitudPeticion): Response<TutoriaRespuesta>

    @POST("tutorias/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: Long, @Body peticion: CancelacionPeticion): Response<TutoriaRespuesta>

    /** Lo puede hacer el estudiante o el tutor, desde que empieza la tutoría hasta 48 h después de terminar. */
    @POST("tutorias/{id}/inasistencia")
    suspend fun reportarInasistencia(@Path("id") id: Long, @Body peticion: InasistenciaPeticion): Response<TutoriaRespuesta>

    // OJO: rutas supuestas (siguen el estilo de `cancelar` e `inasistencia`); confirmarlas con el servidor.
    /** Solo una tutoría COMPLETADA y todavía no calificada por el estudiante. */
    @POST("tutorias/{id}/calificar")
    suspend fun calificar(@Path("id") id: Long, @Body peticion: CalificacionPeticion): Response<MensajeRespuesta>

    @POST("postulaciones")
    suspend fun postularse(@Body peticion: PostulacionPeticion): Response<MensajeRespuesta>
}
