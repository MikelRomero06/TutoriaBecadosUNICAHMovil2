package com.example.tutoriamovil2.red

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.example.tutoriamovil2.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

/**
 * Descarga fotos de la API con el mismo cliente OkHttp (token incluido) y las guarda en memoria.
 * En el emulador, `localhost` y `127.0.0.1` se cambian por `10.0.2.2`.
 */
object CargadorFotos {

    private val cache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** Devuelve la foto reducida a un lado máximo de [ladoMaximo] px, o null si no hay o falla. */
    suspend fun cargar(context: Context, fotoUrl: String?, ladoMaximo: Int = 512): Bitmap? {
        val url = normalizarUrl(fotoUrl) ?: return null
        val clave = "$url@$ladoMaximo"
        cache.get(clave)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val peticion = Request.Builder().url(url).build()
                val bytes = ApiCliente.http(context).newCall(peticion).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.bytes() else null
                } ?: return@withContext null
                decodificar(bytes, ladoMaximo)?.also { cache.put(clave, it) }
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Pone el [marcador] y, si hay foto, la descarga y la muestra en [vista].
     * Sirve en listas: si la vista se recicló para otra foto, no la pisa.
     */
    fun mostrar(
        scope: CoroutineScope,
        vista: ImageView,
        fotoUrl: String?,
        ladoMaximo: Int = 256,
        @DrawableRes marcador: Int = R.drawable.ic_person_placeholder,
    ) {
        val url = normalizarUrl(fotoUrl)
        vista.tag = url
        val enCache = url?.let { cache.get("$it@$ladoMaximo") }
        if (enCache != null) {
            vista.setImageBitmap(enCache)
            return
        }
        vista.setImageResource(marcador)
        if (url == null) return
        scope.launch {
            val foto = cargar(vista.context, url, ladoMaximo)
            if (foto != null && vista.tag == url) vista.setImageBitmap(foto)
        }
    }

    fun normalizarUrl(raw: String?): String? {
        val limpio = raw?.trim().orEmpty()
        if (limpio.isEmpty()) return null
        val base = ApiConfig.BASE_URL.toHttpUrlOrNull() ?: return limpio
        val resuelta = base.resolve(limpio) ?: return null
        return if (resuelta.host == "localhost" || resuelta.host == "127.0.0.1") {
            resuelta.newBuilder().host("10.0.2.2").build().toString()
        } else {
            resuelta.toString()
        }
    }

    private fun decodificar(bytes: ByteArray, ladoMaximo: Int): Bitmap? {
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        var escala = 1
        while (limites.outWidth / (escala * 2) >= ladoMaximo && limites.outHeight / (escala * 2) >= ladoMaximo) {
            escala *= 2
        }
        val opciones = BitmapFactory.Options().apply { inSampleSize = escala }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opciones)
    }
}
