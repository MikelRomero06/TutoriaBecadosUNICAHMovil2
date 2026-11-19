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
import com.example.tutoriamovil2.red.dto.TutoriaDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

sealed class EstadoMisTutorias {
    data object Cargando : EstadoMisTutorias()
    data class Exito(val tutorias: List<TutoriaDto>, val total: Int, val cargandoMas: Boolean) : EstadoMisTutorias()
    data object Vacio : EstadoMisTutorias()
    data class Error(val mensaje: String) : EstadoMisTutorias()
}

class MisTutoriasViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _estado = MutableLiveData<EstadoMisTutorias>(EstadoMisTutorias.Cargando)
    val estado: LiveData<EstadoMisTutorias> = _estado

    /** "proximas" o "historial", como lo espera `GET mis-tutorias?tipo=`. */
    var tipo = PROXIMAS
        private set

    private val tutorias = mutableListOf<TutoriaDto>()
    private var pagina = 1
    private var ultimaPagina = 1
    private var total = 0
    private var consulta: Job? = null

    fun elegirTipo(nuevo: String) {
        if (nuevo == tipo && _estado.value !is EstadoMisTutorias.Error) return
        tipo = nuevo
        cargar()
    }

    fun cargar() {
        consulta?.cancel()
        _estado.value = EstadoMisTutorias.Cargando
        consulta = viewModelScope.launch {
            when (val res = repo.misTutorias(tipo, 1)) {
                is Resultado.Exito -> {
                    tutorias.clear()
                    tutorias.addAll(res.dato.data)
                    pagina = res.dato.meta?.paginaActual ?: 1
                    ultimaPagina = res.dato.meta?.ultimaPagina ?: 1
                    total = res.dato.meta?.total ?: res.dato.data.size
                    publicar()
                }
                is Resultado.ErrorApi -> _estado.value =
                    if (res.codigo == 404) EstadoMisTutorias.Vacio else EstadoMisTutorias.Error(res.mensaje)
                else -> _estado.value = EstadoMisTutorias.Error(res.mensajeError())
            }
        }
    }

    fun cargarMas() {
        val actual = _estado.value
        if (actual !is EstadoMisTutorias.Exito || actual.cargandoMas || pagina >= ultimaPagina) return
        if (consulta?.isActive == true) return

        _estado.value = actual.copy(cargandoMas = true)
        val tipoPedido = tipo
        consulta = viewModelScope.launch {
            when (val res = repo.misTutorias(tipoPedido, pagina + 1)) {
                is Resultado.Exito -> {
                    pagina = res.dato.meta?.paginaActual ?: (pagina + 1)
                    ultimaPagina = res.dato.meta?.ultimaPagina ?: pagina
                    tutorias.addAll(res.dato.data.filter { nueva -> tutorias.none { it.id == nueva.id } })
                    publicar()
                }
                else -> _estado.value = actual.copy(cargandoMas = false)
            }
        }
    }

    private fun publicar() {
        _estado.value = if (tutorias.isEmpty()) {
            EstadoMisTutorias.Vacio
        } else {
            EstadoMisTutorias.Exito(tutorias.toList(), maxOf(total, tutorias.size), cargandoMas = false)
        }
    }

    companion object {
        const val PROXIMAS = "proximas"
        const val HISTORIAL = "historial"
    }
}
