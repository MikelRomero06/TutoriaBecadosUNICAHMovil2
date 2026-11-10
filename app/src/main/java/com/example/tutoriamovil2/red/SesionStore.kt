package com.example.tutoriamovil2.red

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda el token de Sanctum cifrado en disco. Se carga una vez con el contexto
 * de aplicación y después se consulta sin volver a pedirlo.
 */
object SesionStore {

    private const val ARCHIVO = "sesion_segura"
    private const val CLAVE_TOKEN = "token"

    @Volatile
    private var prefs: SharedPreferences? = null

    fun cargar(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: crear(context.applicationContext).also { prefs = it }
        }
    }

    private fun crear(context: Context): SharedPreferences {
        val claveMaestra = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            ARCHIVO,
            claveMaestra,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun guardarToken(token: String) {
        prefs?.edit()?.putString(CLAVE_TOKEN, token)?.apply()
    }

    fun token(): String? = prefs?.getString(CLAVE_TOKEN, null)

    fun hayToken(): Boolean = !token().isNullOrBlank()

    fun borrar() {
        prefs?.edit()?.remove(CLAVE_TOKEN)?.apply()
    }
}
