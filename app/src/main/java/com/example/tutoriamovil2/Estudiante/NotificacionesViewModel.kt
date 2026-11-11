package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.InicioRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

sealed class EstadoAvisos {
    data object Cargando : EstadoAvisos()
    data class Exito(val avisos: List<AvisoInicio>, val sinLeer: Int) : EstadoAvisos()
    data object Vacio : EstadoAvisos()
    data class Error(val mensaje: String) : EstadoAvisos()
}

class NotificacionesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = InicioRepositorio(ApiCliente.notificaciones(app))

    private val _estado = MutableLiveData<EstadoAvisos>(EstadoAvisos.Cargando)
    val estado: LiveData<EstadoAvisos> = _estado

    private val _aviso = MutableLiveData<String?>()
    val aviso: LiveData<String?> = _aviso

    fun cargar() {
        if (_estado.value !is EstadoAvisos.Exito) _estado.value = EstadoAvisos.Cargando
        viewModelScope.launch {
            val citaciones = async { repo.citaciones() }
            val notificaciones = async { repo.notificaciones() }
            val resCitaciones = citaciones.await()
            val resNotificaciones = notificaciones.await()

            val lista = mutableListOf<AvisoInicio>()
            if (resCitaciones is Resultado.Exito) lista += resCitaciones.dato.map(::avisoDesdeCitacion)
            if (resNotificaciones is Resultado.Exito) lista += resNotificaciones.dato.map(::avisoDesdeNotificacion)

            _estado.value = when {
                lista.isNotEmpty() -> EstadoAvisos.Exito(
                    // Primero las citaciones pendientes, después lo no leído.
                    lista.sortedWith(compareByDescending<AvisoInicio> { it.esCitacion && it.pendiente }
                        .thenByDescending { it.pendiente }),
                    lista.count { !it.esCitacion && it.pendiente },
                )
                resCitaciones is Resultado.Exito || resNotificaciones is Resultado.Exito -> EstadoAvisos.Vacio
                else -> EstadoAvisos.Error(resNotificaciones.mensajeError())
            }
        }
    }

    fun marcarEnterado(citacionId: Long) {
        viewModelScope.launch {
            when (val res = repo.marcarEnterado(citacionId)) {
                is Resultado.Exito -> cargar()
                else -> _aviso.value = res.mensajeError()
            }
        }
    }

    fun leerTodas() {
        viewModelScope.launch {
            when (val res = repo.leerTodas()) {
                is Resultado.Exito -> cargar()
                else -> _aviso.value = res.mensajeError()
            }
        }
    }

    fun avisoMostrado() {
        _aviso.value = null
    }
}
