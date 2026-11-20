package com.example.tutoriamovil2.Tutor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.DisponibilidadRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.SesionStore
import com.example.tutoriamovil2.red.dto.DisponibilidadDto
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class EstadoHorarios {
    data object Cargando : EstadoHorarios()

    // guardando = hay un cambio viajando al servidor; mientras tanto la pantalla no deja tocar otra celda.
    data class Listo(val bloques: List<DisponibilidadDto>, val guardando: Boolean = false) : EstadoHorarios()

    data class Error(val mensaje: String) : EstadoHorarios()
}

// Maneja la pantalla "Horarios" del tutor.
// Cada vez que el tutor agrega o quita un horario, se manda la lista completa al servidor (así funciona el API).
// Si el servidor rechaza el cambio, la pantalla se queda con la lista anterior y se muestra el motivo.
class TutorHorariosViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        // Mismo rango que acepta el servidor: de 7:00 a 21:00, de lunes a sábado.
        val HORAS = 7..20
        val DIAS = 1..6
    }

    private val repo = DisponibilidadRepositorio(ApiCliente.disponibilidad(app))

    private val _estado = MutableLiveData<EstadoHorarios>(EstadoHorarios.Cargando)
    val estado: LiveData<EstadoHorarios> = _estado

    // Día que se está viendo. Arranca en el día de hoy (o en lunes si hoy es domingo).
    private val _diaSeleccionado = MutableLiveData(diaDeHoy())
    val diaSeleccionado: LiveData<Int> = _diaSeleccionado

    // Mensajes de una sola vez (un Toast). El fragment lo limpia después de mostrarlo.
    private val _mensaje = MutableLiveData<String?>()
    val mensaje: LiveData<String?> = _mensaje

    private val _sesionCerrada = MutableLiveData(false)
    val sesionCerrada: LiveData<Boolean> = _sesionCerrada

    // La última lista que confirmó el servidor.
    private var bloques: List<DisponibilidadDto> = emptyList()

    // Se pide la lista una sola vez, al crear el ViewModel. Si la pantalla se recrea (girar el
    // teléfono, cambiar de pestaña) se reutiliza lo que ya está cargado.
    init {
        cargar()
    }

    fun cargar() {
        _estado.value = EstadoHorarios.Cargando
        viewModelScope.launch {
            when (val res = repo.obtener()) {
                is Resultado.Exito -> {
                    bloques = res.dato
                    _estado.value = EstadoHorarios.Listo(bloques)
                }
                Resultado.NoAutorizado -> cerrarSesion()
                else -> _estado.value = EstadoHorarios.Error(res.mensajeError())
            }
        }
    }

    fun elegirDia(dia: Int) {
        _diaSeleccionado.value = dia
    }

    fun agregar(dia: Int, hora: Int, modalidad: String, lugarOEnlace: String?) {
        val nuevo = DisponibilidadDto(dia, hora, modalidad, lugarOEnlace?.trim()?.takeIf { it.isNotEmpty() })
        // Si ya había algo en esa celda se reemplaza, para no mandar dos bloques el mismo día y hora.
        guardar(bloques.filterNot { it.diaSemana == dia && it.horaInicio == hora } + nuevo, "Horario publicado")
    }

    fun eliminar(dia: Int, hora: Int) {
        guardar(bloques.filterNot { it.diaSemana == dia && it.horaInicio == hora }, "Horario eliminado")
    }

    fun mensajeMostrado() {
        _mensaje.value = null
    }

    private fun guardar(lista: List<DisponibilidadDto>, textoExito: String) {
        if ((_estado.value as? EstadoHorarios.Listo)?.guardando == true) return
        _estado.value = EstadoHorarios.Listo(bloques, guardando = true)

        viewModelScope.launch {
            val res = repo.guardar(lista.sortedWith(compareBy({ it.diaSemana }, { it.horaInicio })))
            when (res) {
                is Resultado.Exito -> {
                    bloques = res.dato
                    _mensaje.value = textoExito
                }
                Resultado.NoAutorizado -> {
                    cerrarSesion()
                    return@launch
                }
                // En un 422 el mensaje del primer campo con error es más claro que el mensaje general.
                is Resultado.ErrorValidacion ->
                    _mensaje.value = res.errores.values.firstOrNull()?.firstOrNull() ?: res.mensaje
                else -> _mensaje.value = res.mensajeError()
            }
            _estado.value = EstadoHorarios.Listo(bloques)
        }
    }

    private fun cerrarSesion() {
        SesionStore.borrar()
        _sesionCerrada.value = true
    }

    // Calendar cuenta domingo = 1 ... sábado = 7; el servidor usa lunes = 1 ... sábado = 6.
    private fun diaDeHoy(): Int {
        val dia = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1
        return if (dia in DIAS) dia else 1
    }
}
