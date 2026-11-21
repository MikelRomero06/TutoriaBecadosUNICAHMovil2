package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.TutoriasRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import kotlinx.coroutines.launch

sealed class EstadoCancelacion {
    data object Inactivo : EstadoCancelacion()
    data object Enviando : EstadoCancelacion()
    data object Exito : EstadoCancelacion()
    data class Error(val mensaje: String) : EstadoCancelacion()
}

class CancelarTutoriaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _estado = MutableLiveData<EstadoCancelacion>(EstadoCancelacion.Inactivo)
    val estado: LiveData<EstadoCancelacion> = _estado

    fun cancelar(tutoriaId: Long, motivo: String) {
        if (_estado.value is EstadoCancelacion.Enviando) return
        _estado.value = EstadoCancelacion.Enviando
        viewModelScope.launch {
            _estado.value = when (val res = repo.cancelarTutoria(tutoriaId, motivo)) {
                is Resultado.Exito -> EstadoCancelacion.Exito
                else -> EstadoCancelacion.Error(res.mensajeError())
            }
        }
    }

    /** Se llama al mostrar el resultado, para no repetirlo al rotar la pantalla. */
    fun consumir() {
        _estado.value = EstadoCancelacion.Inactivo
    }
}
