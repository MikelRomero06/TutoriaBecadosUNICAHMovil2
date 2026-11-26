package com.example.tutoriamovil2.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.tutoriamovil2.red.PerfilApi
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.SolicitudFotoDto
import com.example.tutoriamovil2.red.dto.SubirFotoRespuesta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class PerfilRepositorio(private val api: PerfilApi, private val context: Context) {

    suspend fun subirFoto(uri: Uri): Resultado<SubirFotoRespuesta> {
        val bytes = withContext(Dispatchers.IO) { comprimir(uri) }
            ?: return Resultado.ErrorApi(0, "No se pudo leer la imagen. Probá con otra.")
        val parte = MultipartBody.Part.createFormData(
            "foto", "foto.jpg", bytes.toRequestBody("image/jpeg".toMediaType()),
        )
        return llamarApi({ api.subirFoto(parte) }) { it }
    }

    suspend fun solicitudPendiente(): Resultado<SolicitudFotoDto?> =
        llamarApi({ api.solicitudFoto() }) { it.solicitud }

    // Las fotos de la cámara pesan varios MB; se bajan a 1024 px en JPEG para no pasar el límite de 5 MB.
    private fun comprimir(uri: Uri, ladoMaximo: Int = 1024): ByteArray? = try {
        val resolver = context.contentResolver
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }

        var muestra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= ladoMaximo) muestra *= 2

        val original = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
        } ?: return null

        val escala = ladoMaximo.toFloat() / maxOf(original.width, original.height)
        val final = if (escala < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * escala).toInt(), (original.height * escala).toInt(), true)
        } else original

        ByteArrayOutputStream().use { salida ->
            final.compress(Bitmap.CompressFormat.JPEG, 85, salida)
            salida.toByteArray()
        }
    } catch (_: Exception) {
        null
    }
}
