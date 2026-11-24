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

sealed class EstadoReporte {
    data object Inactivo : EstadoReporte()
    data object Enviando : EstadoReporte()
    data object Exito : EstadoReporte()
    data class Error(val mensaje: String) : EstadoReporte()
}

/** Envía el reporte de inasistencia. Lo usan tanto el estudiante como el tutor. */
class ReportarInasistenciaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _estado = MutableLiveData<EstadoReporte>(EstadoReporte.Inactivo)
    val estado: LiveData<EstadoReporte> = _estado

    fun reportar(tutoriaId: Long, motivo: String) {
        // Evita mandar el reporte dos veces si tocan el botón seguido.
        if (_estado.value is EstadoReporte.Enviando) return
        _estado.value = EstadoReporte.Enviando
        viewModelScope.launch {
            _estado.value = when (val res = repo.reportarInasistencia(tutoriaId, motivo)) {
                is Resultado.Exito -> EstadoReporte.Exito
                else -> EstadoReporte.Error(res.mensajeError())
            }
        }
    }

    /** Se llama al mostrar el resultado, para no repetirlo al rotar la pantalla. */
    fun consumir() {
        _estado.value = EstadoReporte.Inactivo
    }
}
