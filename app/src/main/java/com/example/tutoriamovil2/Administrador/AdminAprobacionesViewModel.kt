package com.example.tutoriamovil2.Administrador

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.data.AdminRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.CambioFotoDto
import com.example.tutoriamovil2.red.dto.PostulacionDto
import kotlinx.coroutines.launch

enum class TabAprobaciones { FOTOS, POSTULACIONES }

sealed class EstadoLista<out T> {
    data object Cargando : EstadoLista<Nothing>()
    data class Exito<T>(val items: List<T>) : EstadoLista<T>()
    data object Vacio : EstadoLista<Nothing>()
    data class Error(val mensaje: String) : EstadoLista<Nothing>()
}

/** Cambios de foto y postulaciones pendientes, con sus acciones de aprobar y rechazar. */
class AdminAprobacionesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = AdminRepositorio(ApiCliente.admin(app))

    private val _tab = MutableLiveData(TabAprobaciones.FOTOS)
    val tab: LiveData<TabAprobaciones> = _tab

    private val _fotos = MutableLiveData<EstadoLista<CambioFotoDto>>(EstadoLista.Cargando)
    val fotos: LiveData<EstadoLista<CambioFotoDto>> = _fotos

    private val _postulaciones = MutableLiveData<EstadoLista<PostulacionDto>>(EstadoLista.Cargando)
    val postulaciones: LiveData<EstadoLista<PostulacionDto>> = _postulaciones

    /** True mientras se envía una aprobación o rechazo: bloquea los botones de todas las tarjetas. */
    private val _procesando = MutableLiveData(false)
    val procesando: LiveData<Boolean> = _procesando

    /** Aviso para mostrar una vez (Toast); el fragment lo consume con [consumirMensaje]. */
    private val _mensaje = MutableLiveData<String?>(null)
    val mensaje: LiveData<String?> = _mensaje

    private val listaFotos = mutableListOf<CambioFotoDto>()
    private val listaPostulaciones = mutableListOf<PostulacionDto>()

    fun elegirTab(nuevo: TabAprobaciones) {
        _tab.value = nuevo
    }

    fun cargarTodo() {
        cargarFotos()
        cargarPostulaciones()
    }

    fun recargarActual() = if (_tab.value == TabAprobaciones.FOTOS) cargarFotos(forzarCarga = true) else cargarPostulaciones(forzarCarga = true)

    fun cargarFotos(forzarCarga: Boolean = false) {
        // Si ya hay lista en pantalla se refresca sin tapar el contenido con el indicador de carga.
        if (forzarCarga || _fotos.value !is EstadoLista.Exito) _fotos.value = EstadoLista.Cargando
        viewModelScope.launch {
            when (val res = repo.cambiosFoto()) {
                is Resultado.Exito -> {
                    listaFotos.clear()
                    listaFotos.addAll(res.dato)
                    publicarFotos()
                }
                else -> _fotos.value = EstadoLista.Error(res.mensajeError())
            }
        }
    }

    fun cargarPostulaciones(forzarCarga: Boolean = false) {
        if (forzarCarga || _postulaciones.value !is EstadoLista.Exito) _postulaciones.value = EstadoLista.Cargando
        viewModelScope.launch {
            when (val res = repo.postulaciones()) {
                is Resultado.Exito -> {
                    listaPostulaciones.clear()
                    listaPostulaciones.addAll(res.dato)
                    publicarPostulaciones()
                }
                else -> _postulaciones.value = EstadoLista.Error(res.mensajeError())
            }
        }
    }

    fun aprobarFoto(id: Long) = ejecutar(
        llamada = { repo.aprobarFoto(id) },
        exito = R.string.admin_aprobado,
        quitar = { listaFotos.removeAll { it.id == id }; publicarFotos() },
        recargar = { cargarFotos() },
    )

    fun rechazarFoto(id: Long) = ejecutar(
        llamada = { repo.rechazarFoto(id, null) },
        exito = R.string.admin_rechazado,
        quitar = { listaFotos.removeAll { it.id == id }; publicarFotos() },
        recargar = { cargarFotos() },
    )

    fun aprobarPostulacion(id: Long) = ejecutar(
        llamada = { repo.aprobarPostulacion(id) },
        exito = R.string.admin_aprobado,
        quitar = { listaPostulaciones.removeAll { it.id == id }; publicarPostulaciones() },
        recargar = { cargarPostulaciones() },
    )

    fun rechazarPostulacion(id: Long, motivo: String) = ejecutar(
        llamada = { repo.rechazarPostulacion(id, motivo) },
        exito = R.string.admin_rechazado,
        quitar = { listaPostulaciones.removeAll { it.id == id }; publicarPostulaciones() },
        recargar = { cargarPostulaciones() },
    )

    fun consumirMensaje() {
        _mensaje.value = null
    }

    private fun ejecutar(
        llamada: suspend () -> Resultado<Unit>,
        @StringRes exito: Int,
        quitar: () -> Unit,
        recargar: () -> Unit,
    ) {
        // Evita aprobar o rechazar dos veces si tocan seguido.
        if (_procesando.value == true) return
        _procesando.value = true
        viewModelScope.launch {
            when (val res = llamada()) {
                is Resultado.Exito -> {
                    quitar()
                    avisar(exito)
                }
                is Resultado.ErrorApi ->
                    // 409 / 404: otro administrador ya resolvió este caso.
                    if (res.esConflicto || res.esNoEncontrado) {
                        avisar(R.string.admin_ya_resuelta)
                        recargar()
                    } else {
                        _mensaje.value = res.mensaje
                    }
                is Resultado.ErrorValidacion -> _mensaje.value = res.errores["motivo"]?.firstOrNull() ?: res.mensaje
                else -> _mensaje.value = res.mensajeError()
            }
            _procesando.value = false
        }
    }

    private fun avisar(@StringRes texto: Int) {
        _mensaje.value = getApplication<Application>().getString(texto)
    }

    private fun publicarFotos() {
        _fotos.value = if (listaFotos.isEmpty()) EstadoLista.Vacio else EstadoLista.Exito(listaFotos.toList())
    }

    private fun publicarPostulaciones() {
        _postulaciones.value =
            if (listaPostulaciones.isEmpty()) EstadoLista.Vacio else EstadoLista.Exito(listaPostulaciones.toList())
    }
}
