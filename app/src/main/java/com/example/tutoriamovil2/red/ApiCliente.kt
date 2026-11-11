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
    private var retrofit: Retrofit? = null

    @Volatile
    private var authApi: AuthApi? = null

    @Volatile
    private var registroApi: RegistroApi? = null

    @Volatile
    private var tutoriasApi: TutoriasApi? = null

    @Volatile
    private var notificacionesApi: NotificacionesApi? = null

    @Volatile
    private var httpClient: OkHttpClient? = null

    /** Cliente OkHttp compartido (mismos timeouts e interceptores); sirve para descargar imágenes. */
    fun http(context: Context): OkHttpClient {
        return httpClient ?: synchronized(this) {
            httpClient ?: construirCliente(context.applicationContext).also { httpClient = it }
        }
    }

    fun auth(context: Context): AuthApi {
        return authApi ?: synchronized(this) {
            authApi ?: retrofit(context).create(AuthApi::class.java).also { authApi = it }
        }
    }

    fun registro(context: Context): RegistroApi {
        return registroApi ?: synchronized(this) {
            registroApi ?: retrofit(context).create(RegistroApi::class.java).also { registroApi = it }
        }
    }

    private fun retrofit(context: Context): Retrofit {
        return retrofit ?: synchronized(this) {
            retrofit ?: construir(context.applicationContext).also { retrofit = it }
        }
    }

    fun tutorias(context: Context): TutoriasApi {
        return tutoriasApi ?: synchronized(this) {
            tutoriasApi ?: retrofit(context).create(TutoriasApi::class.java).also { tutoriasApi = it }
        }
    }

    fun notificaciones(context: Context): NotificacionesApi {
        return notificacionesApi ?: synchronized(this) {
            notificacionesApi ?: retrofit(context).create(NotificacionesApi::class.java).also { notificacionesApi = it }
        }
    }

    private fun construir(context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(http(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun construirCliente(context: Context): OkHttpClient {
        // Deja el almacén de token listo antes de que el interceptor lo consulte.
        SesionStore.cargar(context)

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor())
            .addInterceptor(SesionExpiradaInterceptor())
            .addInterceptor(logging)
            .build()
    }
}
