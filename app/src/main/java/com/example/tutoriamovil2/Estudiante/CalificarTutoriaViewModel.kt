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

sealed class EstadoCalificacion {
    data object Inactivo : EstadoCalificacion()
    data object Enviando : EstadoCalificacion()
    data object Exito : EstadoCalificacion()
    /** 409: la tutoría ya tenía evaluación (p. ej. se calificó desde otro dispositivo). */
    data object YaCalificada : EstadoCalificacion()
    /** `errores` trae los mensajes del 422 por campo (puntaje, comentario). */
    data class Error(val mensaje: String, val errores: Map<String, List<String>> = emptyMap()) : EstadoCalificacion()
}

/** Envía la evaluación (1 a 5 estrellas y comentario opcional) de una tutoría realizada. */
class CalificarTutoriaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TutoriasRepositorio(ApiCliente.tutorias(app))

    private val _estado = MutableLiveData<EstadoCalificacion>(EstadoCalificacion.Inactivo)
    val estado: LiveData<EstadoCalificacion> = _estado

    fun calificar(tutoriaId: Long, puntaje: Int, comentario: String?) {
        // Evita calificar dos veces si tocan el botón seguido.
        if (_estado.value is EstadoCalificacion.Enviando) return
        _estado.value = EstadoCalificacion.Enviando
        viewModelScope.launch {
            _estado.value = when (val res = repo.calificarTutoria(tutoriaId, puntaje, comentario)) {
                is Resultado.Exito -> EstadoCalificacion.Exito
                is Resultado.ErrorValidacion -> EstadoCalificacion.Error(res.mensaje, res.errores)
                is Resultado.ErrorApi ->
                    if (res.esConflicto) EstadoCalificacion.YaCalificada else EstadoCalificacion.Error(res.mensaje)
                else -> EstadoCalificacion.Error(res.mensajeError())
            }
        }
    }

    /** Se llama al mostrar el resultado, para no repetirlo al rotar la pantalla. */
    fun consumir() {
        _estado.value = EstadoCalificacion.Inactivo
    }
}
