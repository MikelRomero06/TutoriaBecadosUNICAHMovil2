package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.CarrerasRespuesta
import com.example.tutoriamovil2.red.dto.CompletarRegistroPeticion
import com.example.tutoriamovil2.red.dto.CompletarRegistroRespuesta
import com.example.tutoriamovil2.red.dto.EnviarCodigoPeticion
import com.example.tutoriamovil2.red.dto.EnviarCodigoRespuesta
import com.example.tutoriamovil2.red.dto.FotoRespuesta
import com.example.tutoriamovil2.red.dto.VerificarCodigoPeticion
import com.example.tutoriamovil2.red.dto.VerificarCodigoRespuesta
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RegistroApi {

    @POST("auth/registro/enviar-codigo")
    suspend fun enviarCodigo(@Body peticion: EnviarCodigoPeticion): Response<EnviarCodigoRespuesta>

    @POST("auth/registro/verificar-codigo")
    suspend fun verificarCodigo(@Body peticion: VerificarCodigoPeticion): Response<VerificarCodigoRespuesta>

    @POST("auth/registro/completar")
    suspend fun completar(@Body peticion: CompletarRegistroPeticion): Response<CompletarRegistroRespuesta>

    @GET("auth/registro/carreras")
    suspend fun carreras(): Response<CarrerasRespuesta>

    /** Sube la foto obligatoria; usa el token recién emitido por `completar`. */
    @Multipart
    @POST("perfil/foto")
    suspend fun subirFoto(@Part foto: MultipartBody.Part): Response<FotoRespuesta>
}
