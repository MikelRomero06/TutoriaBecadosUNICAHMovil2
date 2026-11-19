package com.example.tutoriamovil2.Login

import android.app.Application
import android.os.Build
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

class LoginViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepositorio(ApiCliente.auth(app))

    private val _estado = MutableLiveData<EstadoLogin>(EstadoLogin.Inicial)
    val estado: LiveData<EstadoLogin> = _estado

    fun iniciarSesion(email: String, password: String) {
        val correo = email.trim()
        if (correo.isEmpty() || password.isEmpty()) {
            _estado.value = EstadoLogin.Error("Ingresá tu correo y contraseña.")
            return
        }

        _estado.value = EstadoLogin.Cargando
        viewModelScope.launch {
            when (val login = repo.login(correo, password, Build.MODEL ?: "Android")) {
                is Resultado.Exito -> {
                    SesionStore.guardarToken(login.dato.token)
                    confirmarSesion()
                }
                is Resultado.ErrorValidacion -> _estado.value = EstadoLogin.Error(primerError(login))
                is Resultado.NoAutorizado -> _estado.value = EstadoLogin.Error("Las credenciales no coinciden.")
                is Resultado.ErrorApi -> _estado.value = EstadoLogin.Error(login.mensaje)
                is Resultado.ErrorRed -> _estado.value = EstadoLogin.Error(login.mensaje)
            }
        }
    }

    /** Confirma con `GET auth/yo` y enruta según el usuario devuelto. */
    private suspend fun confirmarSesion() {
        when (val yo = repo.yo()) {
            is Resultado.Exito -> _estado.value = EstadoLogin.Autenticado(yo.dato.usuario)
            is Resultado.NoAutorizado -> {
                SesionStore.borrar()
                _estado.value = EstadoLogin.Error("La sesión no pudo iniciarse. Intentá de nuevo.")
            }
            is Resultado.ErrorValidacion -> _estado.value = EstadoLogin.Error(yo.mensaje)
            is Resultado.ErrorApi -> _estado.value = EstadoLogin.Error(yo.mensaje)
            is Resultado.ErrorRed -> _estado.value = EstadoLogin.Error(yo.mensaje)
        }
    }

    private fun primerError(error: Resultado.ErrorValidacion): String {
        return error.errores.values.firstOrNull()?.firstOrNull() ?: error.mensaje
    }
}

sealed class EstadoLogin {
    data object Inicial : EstadoLogin()
    data object Cargando : EstadoLogin()
    data class Autenticado(val usuario: UsuarioDto) : EstadoLogin()
    data class Error(val mensaje: String) : EstadoLogin()
}
