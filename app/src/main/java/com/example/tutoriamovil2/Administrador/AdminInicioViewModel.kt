package com.example.tutoriamovil2.Administrador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.AdminRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.AdminResumenDto
import com.example.tutoriamovil2.red.dto.TutoriaDto
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

sealed class EstadoAdminInicio {
    data object Cargando : EstadoAdminInicio()
    data class Exito(val resumen: AdminResumenDto, val tutorias: List<TutoriaDto>) : EstadoAdminInicio()
    data class Error(val mensaje: String) : EstadoAdminInicio()
}

/** Contadores del día y lista de tutorías de hoy. */
class AdminInicioViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AdminRepositorio(ApiCliente.admin(app))

    private val _estado = MutableLiveData<EstadoAdminInicio>(EstadoAdminInicio.Cargando)
    val estado: LiveData<EstadoAdminInicio> = _estado

    fun cargar() {
        // Si ya hay datos en pantalla se refrescan sin tapar el contenido con el indicador de carga.
        if (_estado.value !is EstadoAdminInicio.Exito) _estado.value = EstadoAdminInicio.Cargando
        viewModelScope.launch {
            val resumen = async { repo.resumen() }
            val tutorias = async { repo.tutoriasHoy() }
            val r = resumen.await()
            val t = tutorias.await()
            _estado.value = when {
                r is Resultado.Exito && t is Resultado.Exito ->
                    EstadoAdminInicio.Exito(r.dato, t.dato.sortedBy { it.horaInicio ?: 0 })
                r !is Resultado.Exito -> EstadoAdminInicio.Error(r.mensajeError())
                else -> EstadoAdminInicio.Error(t.mensajeError())
            }
        }
    }
}
