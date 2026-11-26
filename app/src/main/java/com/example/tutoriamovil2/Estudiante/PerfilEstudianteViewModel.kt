package com.example.tutoriamovil2.Estudiante

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.AuthRepositorio
import com.example.tutoriamovil2.data.PerfilRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.CargadorFotos
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.SesionStore
import com.example.tutoriamovil2.red.dto.UsuarioDto
import kotlinx.coroutines.launch

sealed class EstadoPerfil {
    data object Cargando : EstadoPerfil()
    data class Exito(val usuario: UsuarioDto) : EstadoPerfil()
    data class Error(val mensaje: String) : EstadoPerfil()
}

sealed class EstadoFoto {
    data object Inactivo : EstadoFoto()
    data object Subiendo : EstadoFoto()
    data object Aplicada : EstadoFoto()

    // recienEnviada = true solo justo después de subirla, para mostrar el aviso una vez.
    data class Pendiente(val recienEnviada: Boolean) : EstadoFoto()
    data class Error(val mensaje: String) : EstadoFoto()
}

class PerfilEstudianteViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepositorio(ApiCliente.auth(app))

    private val _estadoPerfil = MutableLiveData<EstadoPerfil>(EstadoPerfil.Cargando)
    val estadoPerfil: LiveData<EstadoPerfil> = _estadoPerfil

    private val _sesionCerrada = MutableLiveData(false)
    val sesionCerrada: LiveData<Boolean> = _sesionCerrada

    private val _fotoPerfil = MutableLiveData<Bitmap?>()
    val fotoPerfil: LiveData<Bitmap?> = _fotoPerfil

    private val perfilRepo = PerfilRepositorio(ApiCliente.perfil(app), app)

    private val _estadoFoto = MutableLiveData<EstadoFoto>(EstadoFoto.Inactivo)
    val estadoFoto: LiveData<EstadoFoto> = _estadoFoto

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
        viewModelScope.launch {
            _fotoPerfil.value = CargadorFotos.cargar(getApplication(), fotoUrl)
        }
    }

    fun cargarSolicitudFoto() {
        viewModelScope.launch {
            val res = perfilRepo.solicitudPendiente()
            if (res is Resultado.Exito) {
                _estadoFoto.value = if (res.dato?.estado == "PENDIENTE") EstadoFoto.Pendiente(false) else EstadoFoto.Inactivo
            }
        }
    }

    fun subirFoto(uri: Uri) {
        if (_estadoFoto.value is EstadoFoto.Subiendo) return
        _estadoFoto.value = EstadoFoto.Subiendo
        viewModelScope.launch {
            _estadoFoto.value = when (val res = perfilRepo.subirFoto(uri)) {
                is Resultado.Exito -> {
                    val url = res.dato.fotoUrl
                    if (url != null) {
                        // Era la primera foto: se aplicó de una vez.
                        _fotoPerfil.value = CargadorFotos.cargar(getApplication(), url)
                        EstadoFoto.Aplicada
                    } else {
                        EstadoFoto.Pendiente(true)
                    }
                }
                is Resultado.ErrorValidacion -> EstadoFoto.Error(res.errores["foto"]?.firstOrNull() ?: res.mensaje)
                Resultado.NoAutorizado -> {
                    SesionStore.borrar()
                    _sesionCerrada.value = true
                    EstadoFoto.Inactivo
                }
                else -> EstadoFoto.Error(res.mensajeError())
            }
        }
    }

    // El aviso ya se mostró: no repetirlo al girar la pantalla.
    fun fotoAvisada() {
        _estadoFoto.value = when (_estadoFoto.value) {
            is EstadoFoto.Pendiente -> EstadoFoto.Pendiente(false)
            else -> EstadoFoto.Inactivo
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

