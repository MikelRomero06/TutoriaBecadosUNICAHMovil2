package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.AuthRepositorio
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.SesionStore
import com.example.tutoriamovil2.red.dto.UsuarioDto
import kotlinx.coroutines.launch

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

    fun cerrarSesion() {
        viewModelScope.launch {
            // Avisa al servidor; pase lo que pase, se borra el token local y se sale.
            repo.logout()
            SesionStore.borrar()
            _sesionCerrada.value = true
        }
    }
}

