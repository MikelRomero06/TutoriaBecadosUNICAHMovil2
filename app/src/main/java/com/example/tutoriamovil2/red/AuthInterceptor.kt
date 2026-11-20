package com.example.tutoriamovil2.red

import okhttp3.Interceptor
import okhttp3.Response

/** Agrega `Accept: application/json` siempre y `Authorization: Bearer` cuando hay token. */
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val constructor = chain.request().newBuilder()
            .header("Accept", "application/json")

        SesionStore.token()?.let { token ->
            constructor.header("Authorization", "Bearer $token")
        }

        return chain.proceed(constructor.build())
    }
}
