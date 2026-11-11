package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.TutoriasRepositorio
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.TutoriaDto
import kotlinx.coroutines.launch

sealed class EstadoExplorar {
    data object Cargando : EstadoExplorar()
    data class Exito(val tutorias: List<TutoriaDto>) : EstadoExplorar()
    data class Error(val mensaje: String) : EstadoExplorar()
    data object Vacio : EstadoExplorar()
}

class ExplorarViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _estado = MutableLiveData<EstadoExplorar>(EstadoExplorar.Cargando)
    val estado: LiveData<EstadoExplorar> = _estado

    private var todasLasTutorias = listOf<TutoriaDto>()
    private var busquedaActual = ""
    private var filtroModalidad: String? = null

    fun cargarTutorias() {
        _estado.value = EstadoExplorar.Cargando
        viewModelScope.launch {
            when (val res = repo.obtenerTutorias()) {
                is Resultado.Exito -> {
                    todasLasTutorias = res.dato
                    aplicarFiltros()
                }
                is Resultado.ErrorApi -> _estado.value = EstadoExplorar.Error(res.mensaje)
                is Resultado.ErrorRed -> _estado.value = EstadoExplorar.Error(res.mensaje)
                is Resultado.NoAutorizado -> _estado.value = EstadoExplorar.Error("Sesión vencida. Por favor iniciá sesión nuevamente.")
                is Resultado.ErrorValidacion -> _estado.value = EstadoExplorar.Error(res.mensaje)
            }
        }
    }

    fun buscar(query: String) {
        busquedaActual = query.trim()
        aplicarFiltros()
    }

    fun filtrarModalidad(modalidad: String?) {
        filtroModalidad = modalidad
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        var filtradas = todasLasTutorias

        if (busquedaActual.isNotEmpty()) {
            filtradas = filtradas.filter { item ->
                val tema = item.tema ?: ""
                val descripcion = item.descripcion ?: ""
                val materia = item.materia?.nombre ?: ""
                tema.contains(busquedaActual, ignoreCase = true) ||
                        descripcion.contains(busquedaActual, ignoreCase = true) ||
                        materia.contains(busquedaActual, ignoreCase = true)
            }
        }

        if (!filtroModalidad.isNullOrEmpty()) {
            filtradas = filtradas.filter { item ->
                item.modalidad.equals(filtroModalidad, ignoreCase = true)
            }
        }

        if (filtradas.isEmpty()) {
            _estado.value = EstadoExplorar.Vacio
        } else {
            _estado.value = EstadoExplorar.Exito(filtradas)
        }
    }
}
