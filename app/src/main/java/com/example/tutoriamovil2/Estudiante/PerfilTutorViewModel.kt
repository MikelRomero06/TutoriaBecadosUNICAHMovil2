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

    fun cargarAgenda(tutorId: Long) {
        _agenda.value = EstadoAgenda.Cargando
        viewModelScope.launch {
            when (val res = repo.obtenerDisponibilidad(tutorId)) {
                is Resultado.Exito -> {
                    val bloques = expandirDisponibilidad(res.dato)
                    _agenda.value = EstadoAgenda.Exito(bloques)
                    if (_fecha.value == null || bloques.none { it.fecha == _fecha.value }) {
                        elegirFecha(bloques.firstOrNull { it.disponible }?.fecha ?: bloques.firstOrNull()?.fecha)
                    }
                }
                else -> _agenda.value = EstadoAgenda.Error(res.mensajeError())
            }
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
