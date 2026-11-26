package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.SolicitudFotoRespuesta
import com.example.tutoriamovil2.red.dto.SubirFotoRespuesta
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// Foto de perfil. La primera foto se aplica directo; los cambios después quedan pendientes de aprobación.
interface PerfilApi {

    @Multipart
    @POST("perfil/foto")
    suspend fun subirFoto(@Part foto: MultipartBody.Part): Response<SubirFotoRespuesta>

    @GET("perfil/foto/solicitud")
    suspend fun solicitudFoto(): Response<SolicitudFotoRespuesta>
}
