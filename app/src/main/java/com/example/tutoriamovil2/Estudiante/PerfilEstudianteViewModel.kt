package com.example.tutoriamovil2.Estudiante

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.AuthRepositorio
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.ApiConfig
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.SesionStore
import com.example.tutoriamovil2.red.dto.UsuarioDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

sealed class EstadoPerfil {
    data object Cargando : EstadoPerfil()
    data class Exito(val usuario: UsuarioDto) : EstadoPerfil()
    data class Error(val mensaje: String) : EstadoPerfil()
}

class PerfilEstudianteViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepositorio(ApiCliente.auth(app))

    private val _estadoPerfil = MutableLiveData<EstadoPerfil>(EstadoPerfil.Cargando)
    val estadoPerfil: LiveData<EstadoPerfil> = _estadoPerfil

    private val _sesionCerrada = MutableLiveData(false)
    val sesionCerrada: LiveData<Boolean> = _sesionCerrada

    private val _fotoPerfil = MutableLiveData<Bitmap?>()
    val fotoPerfil: LiveData<Bitmap?> = _fotoPerfil

    fun cargarPerfil() {
        _estadoPerfil.value = EstadoPerfil.Cargando
        viewModelScope.launch {
            when (val res = repo.yo()) {
                is Resultado.Exito -> {
                    _estadoPerfil.value = EstadoPerfil.Exito(res.dato.usuario)
                }
                is Resultado.NoAutorizado -> {
                    SesionStore.borrar()
                    _sesionCerrada.value = true
                }
                is Resultado.ErrorApi -> _estadoPerfil.value = EstadoPerfil.Error(res.mensaje)
                is Resultado.ErrorRed -> _estadoPerfil.value = EstadoPerfil.Error(res.mensaje)
                is Resultado.ErrorValidacion -> _estadoPerfil.value = EstadoPerfil.Error(res.mensaje)
            }
        }
    }

    fun cargarFoto(fotoUrl: String?) {
        val url = normalizarUrlFoto(fotoUrl)
        if (url == null) {
            _fotoPerfil.value = null
            return
        }
        viewModelScope.launch {
            _fotoPerfil.value = withContext(Dispatchers.IO) {
                try {
                    val peticion = Request.Builder().url(url).build()
                    ApiCliente.http(getApplication()).newCall(peticion).execute().use { resp ->
                        if (resp.isSuccessful) {
                            resp.body?.byteStream()?.use { BitmapFactory.decodeStream(it) }
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    /** Resuelve la URL contra la base de la API; en el emulador localhost/127.0.0.1 va por 10.0.2.2. */
    private fun normalizarUrlFoto(raw: String?): String? {
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

    fun cerrarSesion() {
        viewModelScope.launch {
            // Avisa al servidor; pase lo que pase, se borra el token local y se sale.
            repo.logout()
            SesionStore.borrar()
            _sesionCerrada.value = true
        }
    }
}

