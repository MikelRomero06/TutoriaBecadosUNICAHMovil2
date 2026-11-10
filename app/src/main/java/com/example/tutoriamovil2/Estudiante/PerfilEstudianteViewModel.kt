package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.AuthRepositorio
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.SesionStore
import kotlinx.coroutines.launch

class PerfilEstudianteViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AuthRepositorio(ApiCliente.auth(app))

    private val _sesionCerrada = MutableLiveData(false)
    val sesionCerrada: LiveData<Boolean> = _sesionCerrada

    fun cerrarSesion() {
        viewModelScope.launch {
            // Avisa al servidor; pase lo que pase, se borra el token local y se sale.
            repo.logout()
            SesionStore.borrar()
            _sesionCerrada.value = true
        }
    }
}
