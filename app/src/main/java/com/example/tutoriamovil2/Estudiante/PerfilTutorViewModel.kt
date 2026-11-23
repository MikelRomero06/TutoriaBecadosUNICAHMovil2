package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.data.TutoriasRepositorio
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.TutorDto
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Job
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed class EstadoPerfilTutor {
    data object Cargando : EstadoPerfilTutor()
    data class Exito(val tutor: TutorDto) : EstadoPerfilTutor()
    data class Error(val mensaje: String) : EstadoPerfilTutor()
}

sealed class EstadoAgenda {
    data object Cargando : EstadoAgenda()
    data class Exito(val bloques: List<BloqueUi>) : EstadoAgenda()
    data class Error(val mensaje: String) : EstadoAgenda()
}

class PerfilTutorViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _estado = MutableLiveData<EstadoPerfilTutor>(EstadoPerfilTutor.Cargando)
    val estado: LiveData<EstadoPerfilTutor> = _estado

    private var cargadoId: Long? = null

    fun cargar(tutorId: Long, forzar: Boolean = false) {
        if (!forzar && cargadoId == tutorId && _estado.value is EstadoPerfilTutor.Exito) return
        cargadoId = tutorId
        _estado.value = EstadoPerfilTutor.Cargando
        viewModelScope.launch {
            _estado.value = when (val res = repo.obtenerTutor(tutorId)) {
                is Resultado.Exito -> EstadoPerfilTutor.Exito(res.dato)
                is Resultado.ErrorApi ->
                    if (res.esNoEncontrado) EstadoPerfilTutor.Error(getApplication<Application>().getString(R.string.perfil_tutor_ya_no))
                    else EstadoPerfilTutor.Error(res.mensaje)
                else -> EstadoPerfilTutor.Error(res.mensajeError())
            }
        }
    }

    // --- Agenda por bloques ---

    private val _agenda = MutableLiveData<EstadoAgenda>(EstadoAgenda.Cargando)
    val agenda: LiveData<EstadoAgenda> = _agenda

    private val _fecha = MutableLiveData<String?>(null)
    val fecha: LiveData<String?> = _fecha

    private val _bloque = MutableLiveData<BloqueUi?>(null)
    val bloque: LiveData<BloqueUi?> = _bloque

    private var consultaAgenda: Job? = null

    fun cargarAgenda(tutorId: Long) {
        consultaAgenda?.cancel()
        _agenda.value = EstadoAgenda.Cargando
        consultaAgenda = viewModelScope.launch {
            val dia = Calendar.getInstance()
            val formato = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val fechas = (0..7).map {
                val fecha = if (dia.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) formato.format(dia.time) else null
                dia.add(Calendar.DAY_OF_MONTH, 1)
                fecha
            }.filterNotNull()
            val resultados = fechas.map { fecha ->
                async { fecha to repo.bloques(tutorId, fecha) }
            }.awaitAll()
            val error = resultados.map { it.second }.firstOrNull {
                it !is Resultado.Exito && it !is Resultado.ErrorValidacion &&
                    !(it is Resultado.ErrorApi && it.codigo == 422)
            }
            if (error != null) {
                _agenda.value = EstadoAgenda.Error(error.mensajeError())
                return@launch
            }
            val bloques = resultados.flatMap { (fecha, resultado) ->
                if (resultado is Resultado.Exito) resultado.dato.map {
                    BloqueUi(fecha, it.horaInicio, it.horaFin, it.modalidad, it.lugarOEnlace, it.disponible)
                } else emptyList()
            }.sortedWith(compareBy({ it.fecha }, { it.horaInicio }))
            _agenda.value = EstadoAgenda.Exito(bloques)
            // Conserva el día elegido aunque esté lleno, para que se vean sus bloques ocupados.
            if (bloques.none { it.fecha == _fecha.value }) {
                elegirFecha(bloques.firstOrNull { it.disponible }?.fecha ?: bloques.firstOrNull()?.fecha)
            }
            _bloque.value = bloques.firstOrNull { it.clave == _bloque.value?.clave && it.disponible }
        }
    }

    /** Cambiar de día descarta el bloque elegido: el botón Solicitar se apaga. */
    fun elegirFecha(fecha: String?) {
        if (fecha == _fecha.value) return
        _fecha.value = fecha
        _bloque.value = null
    }

    fun elegirBloque(bloque: BloqueUi) {
        if (bloque.disponible) _bloque.value = bloque
    }
}
