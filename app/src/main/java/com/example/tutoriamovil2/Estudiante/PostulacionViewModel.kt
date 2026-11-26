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
import kotlinx.coroutines.launch

sealed class EstadoPostulacion {
    data object Inactivo : EstadoPostulacion()
    data object Enviando : EstadoPostulacion()
    data object Exito : EstadoPostulacion()
    /** `errores` trae los mensajes del 422 por campo (materias, bio). */
    data class Error(val mensaje: String, val errores: Map<String, List<String>> = emptyMap()) : EstadoPostulacion()
}

/** Carga las materias de la carrera del estudiante y envía su postulación a tutor. */
class PostulacionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _materias = MutableLiveData<List<MateriaDto>>(emptyList())
    val materias: LiveData<List<MateriaDto>> = _materias

    private val _cargandoMaterias = MutableLiveData(false)
    val cargandoMaterias: LiveData<Boolean> = _cargandoMaterias

    /** Mensaje si no se pudieron cargar las materias; null si no hubo error. */
    private val _errorMaterias = MutableLiveData<String?>(null)
    val errorMaterias: LiveData<String?> = _errorMaterias

    private val _estado = MutableLiveData<EstadoPostulacion>(EstadoPostulacion.Inactivo)
    val estado: LiveData<EstadoPostulacion> = _estado

    fun cargarMaterias(carreraId: Long, forzar: Boolean = false) {
        if (_cargandoMaterias.value == true) return
        if (!forzar && !_materias.value.isNullOrEmpty()) return
        _cargandoMaterias.value = true
        _errorMaterias.value = null
        viewModelScope.launch {
            val res = repo.obtenerMaterias(carreraId)
            if (res is Resultado.Exito) _materias.value = res.dato
            else _errorMaterias.value = res.mensajeError()
            _cargandoMaterias.value = false
        }
    }

    // Materias marcadas. Viven aquí para no perderlas al girar la pantalla.
    private val elegidas = linkedSetOf<Long>()

    fun estaElegida(id: Long) = id in elegidas

    /** Marca o desmarca la materia y devuelve cómo quedó. */
    fun alternar(id: Long): Boolean = if (elegidas.remove(id)) false else elegidas.add(id)

    fun elegidas(): List<Long> = elegidas.toList()

    fun enviar(materiaIds: List<Long>, bio: String?) {
        // Evita postular dos veces si tocan el botón seguido.
        if (_estado.value is EstadoPostulacion.Enviando) return
        _estado.value = EstadoPostulacion.Enviando
        viewModelScope.launch {
            _estado.value = when (val res = repo.postularse(materiaIds, bio)) {
                is Resultado.Exito -> EstadoPostulacion.Exito
                is Resultado.ErrorValidacion -> EstadoPostulacion.Error(res.mensaje, res.errores)
                // 409: ya hay una postulación en revisión (o ya es tutor).
                is Resultado.ErrorApi ->
                    if (res.esConflicto) EstadoPostulacion.Error(getApplication<Application>().getString(R.string.postulacion_ya_enviada))
                    else EstadoPostulacion.Error(res.mensaje)
                else -> EstadoPostulacion.Error(res.mensajeError())
            }
        }
    }

    /** Se llama al mostrar el resultado, para no repetirlo al rotar la pantalla. */
    fun consumir() {
        _estado.value = EstadoPostulacion.Inactivo
    }
}
