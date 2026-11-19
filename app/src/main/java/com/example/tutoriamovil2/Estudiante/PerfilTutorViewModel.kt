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
import com.example.tutoriamovil2.red.dto.TutorDto
import kotlinx.coroutines.launch

sealed class EstadoPerfilTutor {
    data object Cargando : EstadoPerfilTutor()
    data class Exito(val tutor: TutorDto) : EstadoPerfilTutor()
    data class Error(val mensaje: String) : EstadoPerfilTutor()
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
                    if (res.codigo == 404) EstadoPerfilTutor.Error("Este tutor ya no está disponible.")
                    else EstadoPerfilTutor.Error(res.mensaje)
                else -> EstadoPerfilTutor.Error(res.mensajeError())
            }
        }
    }
}
