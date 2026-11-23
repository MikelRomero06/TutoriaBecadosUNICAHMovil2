package com.example.tutoriamovil2.Estudiante

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.tutoriamovil2.R
import com.example.tutoriamovil2.data.TutoriasRepositorio
import com.example.tutoriamovil2.data.mensajeError
import com.example.tutoriamovil2.red.ApiCliente
import com.example.tutoriamovil2.red.Resultado
import com.example.tutoriamovil2.red.dto.MateriaDto
import com.example.tutoriamovil2.red.dto.SolicitudPeticion
import com.example.tutoriamovil2.red.dto.TutoriaDto
import kotlinx.coroutines.launch

sealed class EstadoSolicitud {
    data object Inactivo : EstadoSolicitud()
    data object Enviando : EstadoSolicitud()
    data class Exito(val tutoria: TutoriaDto) : EstadoSolicitud()
    /** `errores` trae los mensajes del 422 por campo (tema, descripcion, materia_id, fecha, hora_inicio). */
    data class Error(val mensaje: String, val errores: Map<String, List<String>> = emptyMap()) : EstadoSolicitud()
}

class SolicitarTutoriaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _materias = MutableLiveData<List<MateriaDto>>(emptyList())
    val materias: LiveData<List<MateriaDto>> = _materias

    private val _cargandoMaterias = MutableLiveData(false)
    val cargandoMaterias: LiveData<Boolean> = _cargandoMaterias

    /** Mensaje si no se pudieron cargar las materias; null si no hubo error. */
    private val _errorMaterias = MutableLiveData<String?>(null)
    val errorMaterias: LiveData<String?> = _errorMaterias

    private val _estado = MutableLiveData<EstadoSolicitud>(EstadoSolicitud.Inactivo)
    val estado: LiveData<EstadoSolicitud> = _estado

    /** Solo se ofrecen las materias que el tutor da. */
    fun cargarMaterias(tutorId: Long, forzar: Boolean = false) {
        if (_cargandoMaterias.value == true) return
        if (!forzar && !_materias.value.isNullOrEmpty()) return
        _cargandoMaterias.value = true
        _errorMaterias.value = null
        viewModelScope.launch {
            val res = repo.obtenerTutor(tutorId)
            if (res is Resultado.Exito) _materias.value = res.dato.materias.orEmpty()
            else _errorMaterias.value = res.mensajeError()
            _cargandoMaterias.value = false
        }
    }

    fun enviar(peticion: SolicitudPeticion) {
        if (_estado.value is EstadoSolicitud.Enviando) return
        _estado.value = EstadoSolicitud.Enviando
        viewModelScope.launch {
            _estado.value = when (val res = repo.solicitarTutoria(peticion)) {
                is Resultado.Exito -> EstadoSolicitud.Exito(res.dato)
                is Resultado.ErrorValidacion -> EstadoSolicitud.Error(res.mensaje, res.errores)
                // 409: otra persona reservó el bloque mientras este estudiante llenaba el formulario.
                is Resultado.ErrorApi ->
                    if (res.esConflicto) EstadoSolicitud.Error(getApplication<Application>().getString(R.string.solicitar_bloque_ocupado))
                    else EstadoSolicitud.Error(res.mensaje)
                else -> EstadoSolicitud.Error(res.mensajeError())
            }
        }
    }

    fun limpiarEstado() { _estado.value = EstadoSolicitud.Inactivo }
}
