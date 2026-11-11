package com.example.tutoriamovil2.red

import android.content.Context
import com.example.tutoriamovil2.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Arma Retrofit una sola vez y expone las APIs. */
object ApiCliente {

    @Volatile
    private var authApi: AuthApi? = null

    @Volatile
    private var tutoriasApi: TutoriasApi? = null

    fun auth(context: Context): AuthApi {
        return authApi ?: synchronized(this) {
            authApi ?: construir(context.applicationContext).create(AuthApi::class.java).also { authApi = it }
        }
    }

    fun tutorias(context: Context): TutoriasApi {
        return tutoriasApi ?: synchronized(this) {
            tutoriasApi ?: construir(context.applicationContext).create(TutoriasApi::class.java).also { tutoriasApi = it }
        }
    }

    private fun construir(context: Context): Retrofit {
        // Deja el almacén de token listo antes de que el interceptor lo consulte.
        SesionStore.cargar(context)

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        val cliente = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .addInterceptor(SesionExpiradaInterceptor())
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
