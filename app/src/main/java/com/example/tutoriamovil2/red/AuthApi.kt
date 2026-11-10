package com.example.tutoriamovil2.red

import com.example.tutoriamovil2.red.dto.LoginPeticion
import com.example.tutoriamovil2.red.dto.LoginRespuesta
import com.example.tutoriamovil2.red.dto.MensajeRespuesta
import com.example.tutoriamovil2.red.dto.UsuarioRespuesta
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body peticion: LoginPeticion): Response<LoginRespuesta>

    @GET("auth/yo")
    suspend fun yo(): Response<UsuarioRespuesta>

    @POST("auth/logout")
    suspend fun logout(): Response<MensajeRespuesta>
}
